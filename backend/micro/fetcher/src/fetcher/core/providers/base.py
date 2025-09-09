"""
Base Provider Abstract Classes
定义数据提供商的基础抽象接口，参考OpenBB设计理念
"""

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional, TypeVar, Generic
from dataclasses import dataclass, field
from enum import Enum
import asyncio
import logging
from datetime import datetime, timezone

# 类型定义
T = TypeVar('T')
QueryParams = TypeVar('QueryParams')
ResponseData = TypeVar('ResponseData')


class DataCategory(Enum):
    """数据类别枚举"""
    BOND = "bond"
    EQUITY = "equity"
    NEWS = "news"
    MACRO = "macro"
    CRYPTO = "crypto"
    FOREX = "forex"
    COMMODITIES = "commodities"
    ALTERNATIVE = "alternative"


class MarketRegion(Enum):
    """市场区域枚举"""
    GLOBAL = "global"
    US = "us"
    CHINA = "cn"
    EUROPE = "eu"
    ASIA_PACIFIC = "ap"
    EMERGING = "em"

@dataclass
class ProviderConfig:
    """提供商配置"""
    # 核心标识字段
    provider_id: str
    class_path: str
    provider_name: str

    # 连接配置
    base_url: Optional[str] = None
    api_key: Optional[str] = None

    # 性能配置
    rate_limit: int = 100  # 每分钟请求数
    timeout: int = 30  # 超时时间(秒)
    retries: int = 3  # 重试次数

    # 运行状态
    enabled: bool = True
    priority: int = 10  # 优先级，数字越小优先级越高

    # 提供商特定配置参数
    provider_params: Dict[str, Any] = field(default_factory=dict)

    # 支持的数据类型和区域
    supported_categories: List[DataCategory] = field(default_factory=list)
    supported_regions: List[MarketRegion] = field(default_factory=list)

    # HTTP请求头
    custom_headers: Dict[str, str] = field(default_factory=dict)

    def __post_init__(self):
        """初始化后的验证和处理"""
        # 验证必需字段
        if not self.provider_id:
            raise ValueError("provider_id 不能为空")
        if not self.class_path:
            raise ValueError("class_path 不能为空")
        if not self.provider_name:
            raise ValueError("provider_name 不能为空")

        # 验证数值范围
        if self.rate_limit <= 0:
            raise ValueError("rate_limit 必须大于0")
        if self.timeout <= 0:
            raise ValueError("timeout 必须大于0")
        if self.retries < 0:
            raise ValueError("retries 不能小于0")
        if self.priority < 0:
            raise ValueError("priority 不能小于0")

        # 标准化 provider_id（小写，替换特殊字符）
        self.provider_id = self.provider_id.lower().replace(' ', '_').replace('-', '_')

    def supports_category(self, category: DataCategory) -> bool:
        """检查是否支持指定的数据类别"""
        return not self.supported_categories or category in self.supported_categories

    def supports_region(self, region: MarketRegion) -> bool:
        """检查是否支持指定的市场区域"""
        return not self.supported_regions or region in self.supported_regions

    def get_provider_params(self) -> Dict[str, Any]:
        """获取提供商初始化参数，合并通用配置"""
        params = self.provider_params.copy()

        # 将通用配置也加入到参数中，提供商可以选择使用
        common_params = {
            'base_url': self.base_url,
            'api_key': self.api_key,
            'timeout': self.timeout,
            'retries': self.retries,
            'rate_limit': self.rate_limit,
            'custom_headers': self.custom_headers,
        }

        # 只添加非空的通用参数
        for key, value in common_params.items():
            if value is not None:
                params.setdefault(key, value)

        return params

    def is_available(self) -> bool:
        """检查提供商是否可用"""
        return self.enabled

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典格式"""
        return {
            'provider_id': self.provider_id,
            'class_path': self.class_path,
            'provider_name': self.provider_name,
            'base_url': self.base_url,
            'api_key': self.api_key,
            'rate_limit': self.rate_limit,
            'timeout': self.timeout,
            'retries': self.retries,
            'enabled': self.enabled,
            'priority': self.priority,
            'provider_params': self.provider_params,
            'supported_categories': [cat.value for cat in self.supported_categories],
            'supported_regions': [region.value for region in self.supported_regions],
            'custom_headers': self.custom_headers,
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'ProviderConfig':
        """从字典创建配置对象"""
        # 转换枚举字段
        if 'supported_categories' in data:
            data['supported_categories'] = [
                DataCategory(cat) for cat in data['supported_categories']
            ]
        if 'supported_regions' in data:
            data['supported_regions'] = [
                MarketRegion(region) for region in data['supported_regions']
            ]

        return cls(**data)

@dataclass
class DataQuality:
    """数据质量评估"""
    accuracy_score: float = 0.0      # 准确性 0-1
    completeness_score: float = 0.0   # 完整性 0-1
    timeliness_score: float = 0.0     # 时效性 0-1
    confidence_level: float = 0.0     # 可信度 0-1
    data_sources: List[str] = None
    last_updated: datetime = None
    
    def __post_init__(self):
        if self.data_sources is None:
            self.data_sources = []
        if self.last_updated is None:
            self.last_updated = datetime.now(timezone.utc)
    
    @property
    def overall_score(self) -> float:
        """总体质量分数"""
        return (self.accuracy_score + self.completeness_score + 
                self.timeliness_score + self.confidence_level) / 4


@dataclass
class ProviderResponse(Generic[T]):
    """通用提供商响应"""
    data: T
    provider_id: str
    request_id: str
    timestamp: datetime
    data_quality: DataQuality
    metadata: Dict[str, Any] = None
    errors: List[str] = None
    warnings: List[str] = None
    
    def __post_init__(self):
        if self.metadata is None:
            self.metadata = {}
        if self.errors is None:
            self.errors = []
        if self.warnings is None:
            self.warnings = []


class BaseProvider(ABC, Generic[QueryParams, ResponseData]):
    """
    抽象数据提供商基类
    
    所有数据提供商必须继承此类并实现核心方法
    设计理念：
    1. 标准化接口，统一数据获取方式
    2. 错误处理和重试机制
    3. 数据质量评估
    4. 缓存支持
    5. AI友好的数据格式
    """
    
    def __init__(self, **kwargs):
        # 从kwargs中构建ProviderConfig，如果没有传入config的话
        if 'config' in kwargs:
            self.config = kwargs['config']
        else:
            # 从kwargs构建基础配置
            self.config = ProviderConfig(
                provider_id=kwargs.get('provider_id', 'unknown'),
                class_path=kwargs.get('class_path', ''),
                provider_name=kwargs.get('provider_name', 'Unknown Provider'),
                base_url=kwargs.get('base_url'),
                api_key=kwargs.get('api_key'),
                rate_limit=kwargs.get('rate_limit', 100),
                timeout=kwargs.get('timeout', 30),
                retries=kwargs.get('retries', 3),
                enabled=kwargs.get('enabled', True),
                priority=kwargs.get('priority', 10),
                provider_params=kwargs.get('provider_params', {}),
                supported_categories=kwargs.get('supported_categories', []),
                supported_regions=kwargs.get('supported_regions', []),
                custom_headers=kwargs.get('custom_headers', {})
            )
        
        self.logger = logging.getLogger(f"provider.{self.config.provider_id}")
        self._session = None
        self._rate_limiter = None
        self._cache = None
        
    @property
    def provider_id(self) -> str:
        """提供商唯一标识"""
        return self.config.provider_id
    
    @property
    def provider_name(self) -> str:
        """提供商显示名称"""
        return self.config.provider_name
    
    @abstractmethod
    async def validate_credentials(self) -> bool:
        """验证API凭证"""
        pass
    
    @abstractmethod
    async def test_connection(self) -> bool:
        """测试连接"""
        pass
    
    @abstractmethod
    def validate_request(self, params: QueryParams) -> bool:
        """验证请求参数"""
        pass
    
    @abstractmethod
    async def fetch_data(self, params: QueryParams) -> ResponseData:
        """获取数据的核心方法"""
        pass
    
    @abstractmethod
    def normalize_data(self, raw_data: Any) -> ResponseData:
        """标准化原始数据"""
        pass
    
    @abstractmethod
    def assess_data_quality(self, data: ResponseData) -> DataQuality:
        """评估数据质量"""
        pass
    
    async def get_data(self, params: QueryParams) -> ProviderResponse[ResponseData]:
        """
        获取数据的主入口方法
        包含完整的错误处理、重试、质量评估流程
        """
        request_id = self._generate_request_id()
        start_time = datetime.now(timezone.utc)
        
        try:
            # 验证请求参数
            if not self.validate_request(params):
                raise ValueError("Invalid request parameters")
            
            # 检查缓存
            cached_data = await self._get_cached_data(params)
            if cached_data:
                self.logger.debug(f"Cache hit for request {request_id}")
                return cached_data
            
            # 速率限制检查
            await self._check_rate_limit()
            
            # 获取数据（带重试机制）
            raw_data = await self._fetch_with_retry(params)
            
            # 标准化数据
            normalized_data = self.normalize_data(raw_data)
            
            # 评估数据质量
            quality = self.assess_data_quality(normalized_data)
            
            # 构建响应
            response = ProviderResponse(
                data=normalized_data,
                provider_id=self.provider_id,
                request_id=request_id,
                timestamp=start_time,
                data_quality=quality,
                metadata=self._build_metadata(params, raw_data)
            )
            
            # 缓存结果
            await self._cache_data(params, response)
            
            return response
            
        except Exception as e:
            self.logger.error(f"Error fetching data: {str(e)}", exc_info=True)
            raise
    
    async def get_supported_symbols(self, category: DataCategory) -> List[str]:
        """获取支持的标的列表"""
        # 默认实现，子类可重写
        return []
    
    async def get_provider_info(self) -> Dict[str, Any]:
        """获取提供商信息"""
        return {
            "provider_id": self.provider_id,
            "provider_name": self.provider_name,
            "supported_categories": [cat.value for cat in self.config.supported_categories],
            "supported_regions": [region.value for region in self.config.supported_regions],
            "rate_limit": self.config.rate_limit
        }
    
    # 私有辅助方法
    def _generate_request_id(self) -> str:
        """生成请求ID"""
        import uuid
        return str(uuid.uuid4())
    
    async def _get_cached_data(self, params: QueryParams) -> Optional[ProviderResponse[ResponseData]]:
        """获取缓存数据"""
        if not self._cache:
            return None
        # 实现缓存逻辑
        return None
    
    async def _cache_data(self, params: QueryParams, response: ProviderResponse[ResponseData]) -> None:
        """缓存数据"""
        if self._cache:
            # 实现缓存逻辑
            pass
    
    async def _check_rate_limit(self) -> None:
        """检查速率限制"""
        if self._rate_limiter:
            await self._rate_limiter.acquire()
    
    async def _fetch_with_retry(self, params: QueryParams) -> Any:
        """带重试机制的数据获取"""
        last_exception = None
        
        for attempt in range(self.config.retries + 1):
            try:
                return await self.fetch_data(params)
            except Exception as e:
                last_exception = e
                if attempt < self.config.retries:
                    wait_time = 2 ** attempt  # 指数退避
                    self.logger.warning(
                        f"Attempt {attempt + 1} failed, retrying in {wait_time}s: {str(e)}"
                    )
                    await asyncio.sleep(wait_time)
                else:
                    self.logger.error(f"All {self.config.retries + 1} attempts failed")
                    
        raise last_exception
    
    def _build_metadata(self, params: QueryParams, raw_data: Any) -> Dict[str, Any]:
        """构建元数据"""
        return {
            "provider_id": self.provider_id,
            "provider_name": self.provider_name,
            "request_params": str(params),
            "data_timestamp": datetime.now(timezone.utc).isoformat(),
            "raw_data_size": len(str(raw_data)) if raw_data else 0
        }


class EquityProvider(BaseProvider):
    """股票数据提供商基类"""
    
    @abstractmethod
    async def get_historical_data(self, symbol: str, start_date: str, end_date: str, **kwargs) -> Any:
        """获取历史数据"""
        pass
    
    @abstractmethod
    async def get_real_time_quote(self, symbols: List[str], **kwargs) -> Any:
        """获取实时行情"""
        pass
    
    @abstractmethod
    async def get_company_info(self, symbols: List[str], **kwargs) -> Any:
        """获取公司信息"""
        pass
    
    @abstractmethod
    async def screen_stocks(self, criteria: Dict[str, Any], **kwargs) -> Any:
        """股票筛选"""
        pass


class NewsProvider(BaseProvider):
    """新闻数据提供商基类"""
    
    @abstractmethod
    async def get_news(self, query: str, limit: int, **kwargs) -> Any:
        """获取新闻"""
        pass
    
    @abstractmethod
    async def get_news_by_symbol(self, symbols: List[str], **kwargs) -> Any:
        """根据股票代码获取相关新闻"""
        pass
    
    @abstractmethod
    async def analyze_sentiment(self, text: str, **kwargs) -> Any:
        """情感分析"""
        pass


class MacroProvider(BaseProvider):
    """宏观经济数据提供商基类"""
    
    @abstractmethod
    async def get_economic_indicators(self, indicators: List[str], **kwargs) -> Any:
        """获取经济指标"""
        pass
    
    @abstractmethod
    async def get_calendar_events(self, start_date: str, end_date: str, **kwargs) -> Any:
        """获取经济日历事件"""
        pass
