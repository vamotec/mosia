"""统一的数据提供商管理器

单例模式管理所有数据提供商，提供统一的注册、获取和分类接口。
"""

import asyncio
import importlib
from typing import Dict, List, Optional, Any, Type

from fetcher.config.logging import get_logger
from fetcher.core.providers.base import DataCategory, ProviderConfig, MarketRegion

logger = get_logger(__name__)

class ProviderManager:
    """统一的数据提供商管理器（单例模式）"""
    
    _instance: Optional['ProviderManager'] = None
    _initialized: bool = False
    
    def __new__(cls) -> 'ProviderManager':
        """单例模式实现"""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        """初始化管理器"""
        if self._initialized:
            return
            
        self._providers: Dict[str, Any] = {}
        self._categories: Dict[DataCategory, List[str]] = {}
        self._configs: Dict[str, ProviderConfig] = {}
        self._lock = asyncio.Lock()
        
        # 预定义提供商配置
        self._setup_default_providers()
        ProviderManager._initialized = True
    
    def _setup_default_providers(self):
        """设置默认提供商配置"""
        default_providers = [
            ProviderConfig(
                provider_id="yahoo_finance",
                provider_name="Yahoo Finance",
                base_url="https://query1.finance.yahoo.com",
                class_path="fetcher.core.providers.yahoo.provider.YahooFinanceProvider",
                rate_limit=100,
                timeout=30,
                retries=3,
                supported_categories=[DataCategory.EQUITY, DataCategory.CRYPTO, DataCategory.FOREX],
                supported_regions=[MarketRegion.GLOBAL, MarketRegion.US, MarketRegion.ASIA_PACIFIC],
                enabled=True,
                priority=1
            ),
            ProviderConfig(
                provider_id="akshare",
                provider_name="AKShare",
                base_url="https://akshare.akfamily.xyz",
                class_path="fetcher.core.providers.akshare.provider.AKShareProvider",
                rate_limit=60,  # 相对保守的速率限制
                timeout=30,
                retries=3,
                supported_categories=[DataCategory.EQUITY, DataCategory.NEWS, DataCategory.MACRO],
                supported_regions=[MarketRegion.CHINA],
                enabled=True,
                priority=2
            ),
            ProviderConfig(
                provider_id="alpha_vantage",
                provider_name="Alpha Vantage",
                base_url="https://www.alphavantage.co",
                class_path="fetcher.core.providers.alpha_vantage.provider.AlphaVantageProvider",
                rate_limit=5,  # Alpha Vantage 免费 API 限制很低
                timeout=30,
                retries=3,
                supported_categories=[DataCategory.EQUITY, DataCategory.FOREX, DataCategory.CRYPTO],
                supported_regions=[MarketRegion.GLOBAL],
                enabled=False,  # 需要 API 密钥
                priority=3
            ),
            ProviderConfig(
                provider_id="finnhub",
                provider_name="Finnhub",
                base_url="https://finnhub.io",
                class_path="fetcher.core.providers.finnhub.provider.FinnhubProvider",
                rate_limit=60,
                timeout=30,
                retries=3,
                supported_categories=[DataCategory.EQUITY, DataCategory.NEWS],
                supported_regions=[MarketRegion.GLOBAL],
                enabled=False,  # 需要 API 密钥
                priority=4
            ),
            ProviderConfig(
                provider_id="tushare",
                provider_name="Tushare Pro",
                base_url="https://api.tushare.pro",
                class_path="fetcher.core.providers.tushare.provider.TushareProvider",
                rate_limit=200,
                timeout=30,
                retries=3,
                supported_categories=[DataCategory.EQUITY, DataCategory.NEWS, DataCategory.MACRO],
                supported_regions=[MarketRegion.CHINA],
                enabled=False,  # 需要 API token
                priority=8
            ),
            ProviderConfig(
                provider_id="polygon",
                provider_name="Polygon.io",
                base_url="https://api.polygon.io",
                class_path="fetcher.core.providers.polygon.provider.PolygonProvider",
                rate_limit=100,
                timeout=30,
                retries=3,
                supported_categories=[DataCategory.EQUITY, DataCategory.FOREX, DataCategory.CRYPTO],
                supported_regions=[MarketRegion.US, MarketRegion.GLOBAL],
                enabled=False,  # 需要 API 密钥
                priority=5
            ),
            ProviderConfig(
                provider_id="wind",
                provider_name="Wind",
                base_url="https://www.wind.com.cn",
                class_path="fetcher.core.providers.wind.provider.WindProvider",
                rate_limit=1000,  # Wind终端通常没有严格限制
                timeout=30,
                retries=3,
                supported_categories=[DataCategory.EQUITY, DataCategory.BOND, DataCategory.NEWS, DataCategory.MACRO],
                supported_regions=[MarketRegion.CHINA, MarketRegion.ASIA_PACIFIC],
                enabled=False,  # 需要Wind终端授权
                priority=6
            )
        ]
        
        for config in default_providers:
            self._configs[config.provider_id] = config
    
    async def initialize_all(self) -> None:
        """初始化所有启用的提供商"""
        if self._providers:
            logger.info("提供商已初始化，跳过重复初始化")
            return

        logger.info("开始初始化数据提供商...")

        # 按优先级排序
        sorted_configs = sorted(
            [config for config in self._configs.values() if config.enabled],
            key=lambda x: x.priority
        )

        success_count = 0
        for config in sorted_configs:
            try:
                provider = await self._load_provider(config)
                if provider:
                    await self.register_provider(config.provider_id, provider, config.supported_categories)
                    success_count += 1
                    logger.info(f"✅ {config.provider_id} 提供商初始化成功")
                else:
                    logger.warning(f"⚠️ {config.provider_id} 提供商初始化返回None")

            except Exception as e:
                logger.error(f"❌ {config.provider_id} 提供商初始化失败: {e}")
                if config.priority <= 2:  # 核心提供商失败时记录更详细的错误
                    logger.error(f"核心提供商 {config.provider_id} 初始化失败", exc_info=True)

        logger.info(f"数据提供商初始化完成: {success_count}/{len(sorted_configs)} 成功")

    async def _load_provider(self, config: ProviderConfig) -> Optional[Any]:
        """动态加载提供商类"""
        if not config.is_available():
            logger.info(f"提供商 {config.provider_id} 已禁用，跳过加载")
            return None

        try:
            # 分离模块路径和类名
            if '.' not in config.class_path:
                raise ValueError(f"无效的类路径格式: {config.class_path}")

            module_path, class_name = config.class_path.rsplit('.', 1)
            logger.debug(f"正在加载提供商模块: {module_path}.{class_name}")

            # 动态导入模块
            module = importlib.import_module(module_path)
            provider_class: Type = getattr(module, class_name)

            # 获取初始化参数
            init_params = config.get_provider_params()
            logger.debug(f"提供商 {config.provider_id} 初始化参数: {list(init_params.keys())}")

            # 实例化提供商
            provider = provider_class(**init_params)

            # 设置提供商的配置引用（如果需要的话）
            if hasattr(provider, 'config'):
                provider.config = config

            # 如果提供商有initialize方法，调用它
            if hasattr(provider, 'initialize'):
                logger.debug(f"正在初始化提供商: {config.provider_id}")
                await provider.initialize()

            logger.info(f"成功加载提供商: {config.provider_id} ({config.provider_name})")
            return provider

        except ImportError as e:
            logger.warning(f"提供商模块 {config.class_path} 导入失败: {e}")
            return None
        except AttributeError as e:
            logger.error(f"提供商类 {config.class_path} 不存在: {e}")
            return None
        except TypeError as e:
            logger.error(f"提供商 {config.provider_id} 初始化参数错误: {e}")
            return None
        except Exception as e:
            logger.error(f"提供商 {config.provider_id} 实例化失败: {e}")
            return None

    # 辅助函数：批量加载提供商
    async def load_providers(self, configs: List[ProviderConfig]) -> Dict[str, Any]:
        """批量加载提供商"""
        providers = {}

        # 按优先级排序
        sorted_configs = sorted(configs, key=lambda x: x.priority)

        for config in sorted_configs:
            provider = await self._load_provider(config)
            if provider:
                providers[config.provider_id] = provider
            else:
                logger.warning(f"提供商 {config.provider_id} 加载失败")

        logger.info(f"成功加载 {len(providers)} 个提供商")
        return providers

    async def register_provider(
        self,
        provider_id: str,
        provider: Any,
        categories: List[DataCategory] = None
    ) -> None:
        """注册提供商
        
        Args:
            provider_id: 提供商唯一标识
            provider: 提供商实例
            categories: 支持的数据类别
        """
        async with self._lock:
            self._providers[provider_id] = provider
            # 注册到分类中
            if categories:
                for category in categories:
                    if category not in self._categories:
                        self._categories[category] = []
                    if provider_id not in self._categories[category]:
                        self._categories[category].append(provider_id)
            
            # 如果提供商自己定义了categories属性，也使用它
            elif hasattr(provider, 'categories'):
                for category in provider.categories:
                    if isinstance(category, str):
                        try:
                            category = DataCategory(category)
                        except ValueError:
                            logger.warning(f"未知数据类别: {category}")
                            continue
                    
                    if category not in self._categories:
                        self._categories[category] = []
                    if provider_id not in self._categories[category]:
                        self._categories[category].append(provider_id)
            
            logger.debug(f"提供商 {provider_id} 注册成功，类别: {categories}")
    
    def get_provider(self, provider_id: str) -> Optional[Any]:
        """根据ID获取提供商
        
        Args:
            provider_id: 提供商唯一标识
            
        Returns:
            提供商实例或None
        """
        return self._providers.get(provider_id)

    def get_providers_by_category(self, category: DataCategory) -> List[Any]:
        """根据类别获取提供商列表（推荐版本）"""
        provider_ids = self._categories.get(category, [])

        # 收集有效的 (优先级, 提供商) 对
        valid_providers = []
        for provider_id in provider_ids:
            config = self._configs.get(provider_id)
            provider = self._providers.get(provider_id)

            if config and provider and config.enabled:
                valid_providers.append((config.priority, provider))
            elif not config:
                logger.warning(f"提供商 {provider_id} 配置缺失")
            elif not provider:
                logger.warning(f"提供商 {provider_id} 实例缺失")

        # 按优先级排序并返回提供商列表
        valid_providers.sort(key=lambda x: x[0])
        return [provider for _, provider in valid_providers]
    
    def get_best_provider(self, category: DataCategory) -> Optional[Any]:
        """获取指定类别的最佳（优先级最高）提供商
        
        Args:
            category: 数据类别
            
        Returns:
            最佳提供商实例或None
        """
        providers = self.get_providers_by_category(category)
        return providers[0] if providers else None
    
    def list_providers(self) -> Dict[str, Dict[str, Any]]:
        """列出所有已注册的提供商
        
        Returns:
            提供商信息字典
        """
        result = {}
        for provider_id, provider in self._providers.items():
            config = self._configs.get(provider_id)
            categories = []
            
            # 查找提供商支持的类别
            for category, provider_ids in self._categories.items():
                if provider_id in provider_ids:
                    categories.append(category.value)
            
            result[provider_id] = {
                "provider": provider.__class__.__name__ if provider else "Unknown",
                "categories": categories,
                "priority": config.priority if config else 999,
                "enabled": config.enabled if config else True,
                "status": "active" if provider else "inactive"
            }
        
        return result
    
    def get_provider_status(self) -> Dict[str, Any]:
        """获取提供商状态信息
        
        Returns:
            状态信息字典
        """
        active_count = len(self._providers)
        total_configured = len([c for c in self._configs.values() if c.enabled])
        
        category_stats = {}
        for category, provider_ids in self._categories.items():
            active_providers = [pid for pid in provider_ids if pid in self._providers]
            category_stats[category.value] = {
                "total": len(provider_ids),
                "active": len(active_providers),
                "providers": active_providers
            }
        
        return {
            "total_providers": active_count,
            "configured_providers": total_configured,
            "initialization_rate": f"{active_count}/{total_configured}",
            "categories": category_stats,
            "health": "healthy" if active_count > 0 else "unhealthy"
        }
    
    async def health_check(self) -> Dict[str, Any]:
        """执行提供商健康检查
        
        Returns:
            健康检查结果
        """
        results = {}
        
        for provider_id, provider in self._providers.items():
            try:
                # 如果提供商有health_check方法，调用它
                if hasattr(provider, 'health_check'):
                    result = await provider.health_check()
                    results[provider_id] = {
                        "status": "healthy" if result else "unhealthy",
                        "details": result if isinstance(result, dict) else {}
                    }
                else:
                    # 简单检查：提供商是否可访问
                    results[provider_id] = {
                        "status": "healthy",
                        "details": {"message": "No health_check method, assuming healthy"}
                    }
                    
            except Exception as e:
                results[provider_id] = {
                    "status": "unhealthy",
                    "details": {"error": str(e)}
                }
        
        return results
    
    async def close_all(self) -> None:
        """关闭所有提供商连接"""
        async with self._lock:
            logger.info("开始关闭所有提供商...")
            
            for provider_id, provider in self._providers.items():
                try:
                    if hasattr(provider, 'close'):
                        await provider.close()
                    logger.debug(f"提供商 {provider_id} 已关闭")
                except Exception as e:
                    logger.error(f"关闭提供商 {provider_id} 时出错: {e}")
            
            self._providers.clear()
            self._categories.clear()
            logger.info("所有提供商已关闭")


# 全局单例实例
provider_manager = ProviderManager()


# 便捷函数

async def initialize_providers() -> None:
    """初始化所有提供商的便捷函数"""
    await provider_manager.initialize_all()


def get_provider(provider_id: str) -> Optional[Any]:
    """获取提供商的便捷函数"""
    return provider_manager.get_provider(provider_id)


def get_providers_by_category(category: DataCategory) -> List[Any]:
    """按类别获取提供商的便捷函数"""
    return provider_manager.get_providers_by_category(category)


def get_best_provider(category: DataCategory) -> Optional[Any]:
    """获取最佳提供商的便捷函数"""
    return provider_manager.get_best_provider(category)


async def close_providers() -> None:
    """关闭所有提供商的便捷函数"""
    await provider_manager.close_all()