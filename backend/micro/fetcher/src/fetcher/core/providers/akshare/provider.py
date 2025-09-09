"""
AKShare Provider Implementation
AKShare数据提供商实现 - 中国金融数据
"""

import asyncio
from datetime import datetime
from typing import Any, Dict, List, Optional

import akshare as ak
import pandas as pd

from fetcher.config.logging import get_logger
from fetcher.core.models.base import EnhancedPriceData, TechnicalIndicators, AIFeatures, CurrencyCode
from fetcher.core.providers.base import EquityProvider, NewsProvider, DataCategory, DataQuality, MarketRegion

logger = get_logger(__name__)

class AKShareProvider(EquityProvider, NewsProvider):
    """AKShare数据提供商 - 专注中国市场"""
    def __init__(self, cache_enabled: bool = True, cache_ttl: int = 300,
                 api_version: str = "v8", **kwargs):
        # 设置provider标识信息，供BaseProvider使用
        kwargs.setdefault('provider_id', 'akshare')
        kwargs.setdefault('provider_name', 'AKShare')  
        kwargs.setdefault('class_path', 'fetcher.core.providers.akshare.provider.AKShareProvider')
        kwargs.setdefault('supported_categories', [DataCategory.EQUITY, DataCategory.NEWS])
        kwargs.setdefault('supported_regions', [MarketRegion.CHINA])
        kwargs.setdefault('priority', 10)
        
        super().__init__(**kwargs)
        self.cache_enabled = cache_enabled
        self.cache_ttl = cache_ttl
        self.api_version = api_version

    async def initialize(self):
        """初始化缓存等资源"""
        if self.cache_enabled:
            logger.info(f"AKShare 提供商启用缓存，TTL: {self.cache_ttl}秒")
    
    async def validate_credentials(self) -> bool:
        """验证凭证（AKShare免费使用）"""
        return True
    
    async def test_connection(self) -> bool:
        """测试连接"""
        try:
            # 测试获取一个简单的数据
            df = ak.stock_zh_a_spot_em()  # 获取A股实时数据
            return not df.empty
        except Exception as e:
            self.logger.error(f"Connection test failed: {e}")
            return False
    
    def validate_request(self, params: Dict[str, Any]) -> bool:
        """验证请求参数"""
        data_type = params.get('data_type', 'historical')
        
        if data_type == 'historical':
            return 'symbol' in params
        elif data_type == 'quote':
            return 'symbol' in params or 'symbols' in params
        elif data_type == 'news':
            return True  # 新闻可以无特定参数
        
        return False
    
    async def fetch_data(self, params: Dict[str, Any]) -> Any:
        """获取原始数据"""
        data_type = params.get('data_type', 'historical')
        
        # 在线程池中运行同步的akshare调用
        if data_type == 'historical':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_historical_data_sync, params
            )
        elif data_type == 'quote':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_quote_data_sync, params
            )
        elif data_type == 'news':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_news_data_sync, params
            )
        elif data_type == 'company_info':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_company_info_sync, params
            )
        else:
            raise ValueError(f"Unsupported data type: {data_type}")
    
    def _fetch_historical_data_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取历史数据"""
        symbol = params['symbol']
        start_date = params.get('start_date', '20220101')
        end_date = params.get('end_date', datetime.now().strftime('%Y%m%d'))
        period = params.get('period', 'daily')
        adjust = params.get('adjust', 'qfq')  # 前复权
        
        try:
            # 根据市场类型选择不同的接口
            if self._is_a_share(symbol):
                # A股数据
                df = ak.stock_zh_a_hist(
                    symbol=symbol,
                    period=period,
                    start_date=start_date,
                    end_date=end_date,
                    adjust=adjust
                )
            elif self._is_hk_share(symbol):
                # 港股数据
                hk_symbol = symbol.replace('.HK', '')
                df = ak.stock_hk_hist(
                    symbol=hk_symbol,
                    period=period,
                    start_date=start_date,
                    end_date=end_date,
                    adjust=adjust
                )
            else:
                raise ValueError(f"Unsupported symbol format: {symbol}")
            
            if df.empty:
                raise ValueError(f"No data found for symbol: {symbol}")
            
            # 标准化列名
            column_mapping = {
                '日期': 'date',
                '开盘': 'open',
                '收盘': 'close',
                '最高': 'high',
                '最低': 'low',
                '成交量': 'volume',
                '成交额': 'amount',
                '涨跌幅': 'change_percent',
                '涨跌额': 'change',
                '换手率': 'turnover_rate'
            }
            
            df = df.rename(columns=column_mapping)
            
            # 确保日期列
            if 'date' in df.columns:
                df['date'] = pd.to_datetime(df['date'])
            else:
                df.reset_index(inplace=True)
                if 'date' not in df.columns and '日期' in df.columns:
                    df['date'] = pd.to_datetime(df['日期'])
            
            data_list = []
            for _, row in df.iterrows():
                data_point = {
                    'timestamp': row['date'].isoformat(),
                    'open': float(row.get('open', 0)) if pd.notna(row.get('open')) else None,
                    'high': float(row.get('high', 0)) if pd.notna(row.get('high')) else None,
                    'low': float(row.get('low', 0)) if pd.notna(row.get('low')) else None,
                    'close': float(row.get('close', 0)) if pd.notna(row.get('close')) else None,
                    'volume': int(row.get('volume', 0)) if pd.notna(row.get('volume')) else None,
                    'amount': float(row.get('amount', 0)) if pd.notna(row.get('amount')) else None,
                    'change': float(row.get('change', 0)) if pd.notna(row.get('change')) else None,
                    'change_percent': float(row.get('change_percent', 0)) if pd.notna(row.get('change_percent')) else None,
                    'turnover_rate': float(row.get('turnover_rate', 0)) if pd.notna(row.get('turnover_rate')) else None
                }
                data_list.append(data_point)
            
            return {
                'symbol': symbol,
                'data': data_list,
                'meta': {
                    'currency': 'CNY' if self._is_a_share(symbol) else 'HKD',
                    'exchange': self._get_exchange(symbol),
                    'adjust_type': adjust,
                    'period': period
                }
            }
            
        except Exception as e:
            self.logger.error(f"Failed to fetch historical data for {symbol}: {e}")
            raise
    
    def _fetch_quote_data_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取实时行情"""
        symbol = params.get('symbol')
        symbols = params.get('symbols', [symbol] if symbol else [])
        
        results = []
        
        try:
            # 获取A股实时数据
            df_a = ak.stock_zh_a_spot_em()
            
            # 获取港股实时数据（如果需要）
            df_hk = None
            has_hk_symbols = any(self._is_hk_share(s) for s in symbols if s)
            if has_hk_symbols:
                try:
                    df_hk = ak.stock_hk_spot_em()
                except:
                    pass  # 港股数据可能不可用
            
            for sym in symbols:
                if not sym:
                    continue
                    
                quote_data = None
                
                if self._is_a_share(sym):
                    # A股查询
                    match = df_a[df_a['代码'] == sym]
                    if not match.empty:
                        row = match.iloc[0]
                        quote_data = self._parse_a_stock_quote(row, sym)
                
                elif self._is_hk_share(sym) and df_hk is not None:
                    # 港股查询
                    hk_code = sym.replace('.HK', '')
                    match = df_hk[df_hk['代码'] == hk_code]
                    if not match.empty:
                        row = match.iloc[0]
                        quote_data = self._parse_hk_stock_quote(row, sym)
                
                if quote_data:
                    results.append(quote_data)
                else:
                    self.logger.warning(f"No quote data found for symbol: {sym}")
            
            return {'quotes': results}
            
        except Exception as e:
            self.logger.error(f"Failed to fetch quote data: {e}")
            raise
    
    def _fetch_news_data_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取新闻数据"""
        try:
            # 获取财经新闻
            news_list = []
            
            # 东方财富新闻
            try:
                df_news = ak.stock_news_em()
                for _, row in df_news.iterrows():
                    news_item = {
                        'title': row.get('新闻标题', ''),
                        'summary': row.get('新闻内容', '')[:200] if row.get('新闻内容') else '',
                        'publish_time': row.get('发布时间', ''),
                        'source': '东方财富',
                        'url': row.get('新闻链接', ''),
                        'category': '财经新闻',
                        'language': 'zh_cn'
                    }
                    news_list.append(news_item)
            except Exception as e:
                self.logger.warning(f"Failed to fetch news from eastmoney: {e}")
            
            return {'news': news_list[:50]}  # 限制返回数量
            
        except Exception as e:
            self.logger.error(f"Failed to fetch news data: {e}")
            raise
    
    def _fetch_company_info_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取公司信息"""
        symbol = params['symbol']
        
        try:
            if self._is_a_share(symbol):
                # A股公司信息
                df_info = ak.stock_individual_info_em(symbol=symbol)
                
                info_dict = {}
                for _, row in df_info.iterrows():
                    key = row.get('item', '')
                    value = row.get('value', '')
                    info_dict[key] = value
                
                return {
                    'symbol': symbol,
                    'company_name': info_dict.get('公司名称', ''),
                    'industry': info_dict.get('所属行业', ''),
                    'business_summary': info_dict.get('经营范围', ''),
                    'market_cap': self._parse_number(info_dict.get('总市值', '')),
                    'pe_ratio': self._parse_number(info_dict.get('市盈率-动态', '')),
                    'pb_ratio': self._parse_number(info_dict.get('市净率', '')),
                    'employees': self._parse_number(info_dict.get('员工人数', '')),
                    'currency': 'CNY',
                    'exchange': self._get_exchange(symbol)
                }
            else:
                # 其他市场暂不支持详细信息
                return {
                    'symbol': symbol,
                    'currency': 'HKD' if self._is_hk_share(symbol) else 'USD',
                    'exchange': self._get_exchange(symbol)
                }
                
        except Exception as e:
            self.logger.error(f"Failed to fetch company info for {symbol}: {e}")
            raise
    
    def _parse_a_stock_quote(self, row, symbol: str) -> Dict[str, Any]:
        """解析A股行情数据"""
        return {
            'symbol': symbol,
            'name': row.get('名称', ''),
            'current_price': float(row.get('最新价', 0)),
            'open': float(row.get('今开', 0)),
            'high': float(row.get('最高', 0)),
            'low': float(row.get('最低', 0)),
            'previous_close': float(row.get('昨收', 0)),
            'change': float(row.get('涨跌额', 0)),
            'change_percent': float(row.get('涨跌幅', 0)),
            'volume': int(row.get('成交量', 0)),
            'amount': float(row.get('成交额', 0)),
            'turnover_rate': float(row.get('换手率', 0)),
            'pe_ratio': float(row.get('市盈率-动态', 0)) if row.get('市盈率-动态') != '-' else None,
            'pb_ratio': float(row.get('市净率', 0)) if row.get('市净率') != '-' else None,
            'market_cap': float(row.get('总市值', 0)),
            'currency': 'CNY',
            'exchange': self._get_exchange(symbol),
            'last_trade_time': datetime.now().isoformat()
        }

    @staticmethod
    def _parse_hk_stock_quote(row, symbol: str) -> Dict[str, Any]:
        """解析港股行情数据"""
        return {
            'symbol': symbol,
            'name': row.get('名称', ''),
            'current_price': float(row.get('最新价', 0)),
            'open': float(row.get('今开', 0)),
            'high': float(row.get('最高', 0)),
            'low': float(row.get('最低', 0)),
            'previous_close': float(row.get('昨收', 0)),
            'change': float(row.get('涨跌额', 0)),
            'change_percent': float(row.get('涨跌幅', 0)),
            'volume': int(row.get('成交量', 0)),
            'amount': float(row.get('成交额', 0)),
            'turnover_rate': float(row.get('换手率', 0)),
            'market_cap': float(row.get('总市值', 0)) if '总市值' in row else None,
            'currency': 'HKD',
            'exchange': 'HKEX',
            'last_trade_time': datetime.now().isoformat()
        }
    
    def normalize_data(self, raw_data: Any) -> List[EnhancedPriceData]:
        """标准化数据"""
        if isinstance(raw_data, dict) and 'data' in raw_data:
            # 历史数据
            return self._normalize_historical_data(raw_data)
        elif isinstance(raw_data, dict) and 'quotes' in raw_data:
            # 行情数据
            return self._normalize_quote_data(raw_data)
        else:
            raise ValueError("Unsupported raw data format")
    
    def _normalize_historical_data(self, raw_data: Dict) -> List[EnhancedPriceData]:
        """标准化历史数据"""
        symbol = raw_data['symbol']
        currency_str = raw_data.get('meta', {}).get('currency', 'CNY')
        
        try:
            currency = CurrencyCode(currency_str)
        except ValueError:
            currency = CurrencyCode.CNY
        
        normalized_data = []
        data_points = raw_data['data']
        
        for i, point in enumerate(data_points):
            price_data = EnhancedPriceData(
                timestamp=datetime.fromisoformat(point['timestamp']),
                symbol=symbol,
                provider_id=self.provider_id,
                open_value=point.get('open'),
                high_value=point.get('high'),
                low_value=point.get('low'),
                close_value=point.get('close'),
                volume=point.get('volume'),
                currency=currency
            )
            
            # 添加中国市场特有字段
            if point.get('amount'):
                price_data.custom_fields['amount'] = point['amount']
            if point.get('turnover_rate'):
                price_data.custom_fields['turnover_rate'] = point['turnover_rate']
            
            # 计算技术指标
            if i >= 20:
                price_data.technical_indicators = self._calculate_technical_indicators(
                    data_points[max(0, i-200):i+1]
                )
            
            # 计算AI特征
            price_data.ai_features = self._calculate_ai_features(
                data_points[max(0, i-20):i+1], i
            )
            
            # 添加AI元数据
            price_data.ai_metadata.add_semantic_tag("provider", "akshare")
            price_data.ai_metadata.add_semantic_tag("market", "china")
            price_data.ai_metadata.add_semantic_tag("currency", currency_str)
            if self._is_a_share(symbol):
                price_data.ai_metadata.add_semantic_tag("market_type", "a_share")
            elif self._is_hk_share(symbol):
                price_data.ai_metadata.add_semantic_tag("market_type", "hk_share")
            
            normalized_data.append(price_data)
        
        return normalized_data
    
    def _normalize_quote_data(self, raw_data: Dict) -> List[EnhancedPriceData]:
        """标准化行情数据"""
        normalized_data = []
        
        for quote in raw_data['quotes']:
            currency = CurrencyCode.CNY if quote['currency'] == 'CNY' else CurrencyCode.HKD
            
            price_data = EnhancedPriceData(
                timestamp=datetime.now(),
                symbol=quote['symbol'],
                provider_id=self.provider_id,
                open_value=quote.get('open'),
                high_value=quote.get('high'),
                low_value=quote.get('low'),
                close_value=quote.get('current_price'),
                volume=quote.get('volume'),
                currency=currency,
                change=quote.get('change'),
                change_percent=quote.get('change_percent')
            )
            
            # 添加扩展字段
            price_data.custom_fields.update({
                'name': quote.get('name', ''),
                'amount': quote.get('amount'),
                'turnover_rate': quote.get('turnover_rate'),
                'pe_ratio': quote.get('pe_ratio'),
                'pb_ratio': quote.get('pb_ratio'),
                'market_cap': quote.get('market_cap')
            })
            
            # AI元数据
            price_data.ai_metadata.add_semantic_tag("provider", "akshare")
            price_data.ai_metadata.add_semantic_tag("data_type", "realtime")
            price_data.ai_metadata.add_semantic_tag("market", "china")
            
            normalized_data.append(price_data)
        
        return normalized_data
    
    def assess_data_quality(self, data: List[EnhancedPriceData]) -> DataQuality:
        """评估数据质量"""
        if not data:
            return DataQuality(
                accuracy_score=0.0,
                completeness_score=0.0,
                timeliness_score=0.8,  # AKShare实时性较好
                confidence_level=0.85,
                data_sources=["akshare"]
            )
        
        # 计算完整性
        total_fields = len(data) * 5
        complete_fields = sum(
            sum(1 for field in [dp.open_value, dp.high_value, dp.low_value, dp.close_value, dp.volume] 
                if field is not None) for dp in data
        )
        completeness_score = complete_fields / total_fields if total_fields > 0 else 0.0
        
        # 时效性评估
        latest_time = max(dp.timestamp for dp in data) if data else datetime.now()
        time_diff = datetime.now() - latest_time.replace(tzinfo=None)
        timeliness_score = max(0.0, 1.0 - time_diff.total_seconds() / 1800)  # 30分钟内满分
        
        return DataQuality(
            accuracy_score=0.9,   # AKShare数据准确性较高
            completeness_score=completeness_score,
            timeliness_score=timeliness_score,
            confidence_level=0.85,
            data_sources=["akshare"],
            last_updated=datetime.now()
        )
    
    # 辅助方法
    @staticmethod
    def _is_a_share(symbol: str) -> bool:
        """判断是否为A股"""
        return len(symbol) == 6 and symbol.isdigit()

    @staticmethod
    def _is_hk_share(symbol: str) -> bool:
        """判断是否为港股"""
        return symbol.endswith('.HK')
    
    def _get_exchange(self, symbol: str) -> str:
        """获取交易所"""
        if self._is_a_share(symbol):
            if symbol.startswith('0') or symbol.startswith('3'):
                return 'SZSE'  # 深圳证券交易所
            else:
                return 'SSE'   # 上海证券交易所
        elif self._is_hk_share(symbol):
            return 'HKEX'
        else:
            return 'UNKNOWN'

    @staticmethod
    def _parse_number(value: str) -> Optional[float]:
        """解析数字字符串"""
        if not value or value == '-' or value == '--':
            return None
        try:
            # 处理中文数字单位
            value = str(value).replace('万', '0000').replace('亿', '00000000')
            return float(value)
        except (ValueError, TypeError):
            return None
    
    def _calculate_technical_indicators(self, data_points: List[Dict]) -> TechnicalIndicators:
        """计算技术指标"""
        # 简化实现
        return TechnicalIndicators()

    def _calculate_ai_features(self, data_points: List[Dict], current_index: int) -> AIFeatures:
        """计算AI特征"""
        # 简化实现
        return AIFeatures()
    
    # 实现抽象方法
    async def get_historical_data(self, symbol: str, start_date: str, end_date: str, **kwargs) -> Any:
        """获取历史数据"""
        params = {
            'symbol': symbol,
            'data_type': 'historical',
            'start_date': start_date.replace('-', ''),
            'end_date': end_date.replace('-', ''),
            **kwargs
        }
        return await self.get_data(params)
    
    async def get_real_time_quote(self, symbols: List[str], **kwargs) -> Any:
        """获取实时行情"""
        params = {
            'symbols': symbols,
            'data_type': 'quote',
            **kwargs
        }
        return await self.get_data(params)
    
    async def get_company_info(self, symbols: List[str], **kwargs) -> Any:
        """获取公司信息"""
        results = []
        for symbol in symbols:
            params = {
                'symbol': symbol,
                'data_type': 'company_info',
                **kwargs
            }
            result = await self.get_data(params)
            results.append(result)
        return results
    
    async def screen_stocks(self, criteria: Dict[str, Any], **kwargs) -> Any:
        """股票筛选"""
        params = {
            'data_type': 'screen',
            'criteria': criteria,
            **kwargs
        }
        return await self.get_data(params)
    
    async def get_news(self, query: str, limit: int, **kwargs) -> Any:
        """获取新闻"""
        params = {
            'data_type': 'news',
            'query': query,
            'limit': limit,
            **kwargs
        }
        return await self.get_data(params)
    
    async def get_news_by_symbol(self, symbols: List[str], **kwargs) -> Any:
        """根据股票代码获取相关新闻"""
        return await self.get_news("", 50, **kwargs)
    
    async def analyze_sentiment(self, text: str, **kwargs) -> Any:
        """情感分析"""
        return {
            'sentiment': 'neutral',
            'score': 0.0,
            'confidence': 0.5
        }
    
    async def get_supported_symbols(self, category: DataCategory) -> List[str]:
        """获取支持的标的列表"""
        if category == DataCategory.EQUITY:
            return [
                # A股
                '000001', '000002', '000858', '600000', '600036', '600519', '600887',
                '000725', '002415', '300014', '002142', '600276', '000338', '002304',
                # 港股
                '0700.HK', '9988.HK', '3690.HK', '0941.HK', '1299.HK', '0388.HK'
            ]
        else:
            return []