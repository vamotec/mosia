"""
Polygon Provider Implementation
Polygon数据提供商实现 - 美股实时和历史数据
"""

from datetime import datetime, timezone, timedelta
from typing import Any, Dict, List
from urllib.parse import urlencode

import aiohttp

from fetcher.config.logging import get_logger
from fetcher.core.models.base import EnhancedPriceData, TechnicalIndicators, AIFeatures, CurrencyCode
from fetcher.core.providers.base import EquityProvider, DataCategory, DataQuality

logger = get_logger(__name__)

class PolygonProvider(EquityProvider):
    """Polygon数据提供商 - 美股实时和历史数据"""
    def __init__(self, cache_enabled: bool = True, cache_ttl: int = 300,
                 api_version: str = "v8", **kwargs):
        kwargs.setdefault('provider_id', 'polygon')
        kwargs.setdefault('provider_name', 'Polygon')
        kwargs.setdefault('class_path', 'fetcher.core.providers.polygon.provider.PolygonProvider')
        kwargs.setdefault('base_url', 'https://api.polygon.io')
        kwargs.setdefault('supported_categories', ['equity', 'option', 'crypto', 'forex'])
        kwargs.setdefault('supported_regions', ['US'])
        super().__init__(**kwargs)
        self.cache_enabled = cache_enabled
        self.cache_ttl = cache_ttl
        self.api_version = api_version

    async def initialize(self):
        """初始化缓存等资源"""
        if self.cache_enabled:
            logger.info(f"Finnhub 提供商启用缓存，TTL: {self.cache_ttl}秒")

    async def validate_credentials(self) -> bool:
        """验证API凭证"""
        try:
            params = {'apikey': self.config.api_key}
            url = f"{self.config.base_url}/v2/aggs/ticker/AAPL/prev?" + urlencode(params)
            
            async with aiohttp.ClientSession() as session:
                async with session.get(url, timeout=self.config.timeout) as response:
                    if response.status == 200:
                        data = await response.json()
                        return data.get('status') == 'OK'
                    return False
        except Exception as e:
            self.logger.error(f"Credential validation failed: {e}")
            return False
    
    async def test_connection(self) -> bool:
        """测试连接"""
        if not self.config.api_key:
            return False
        return await self.validate_credentials()
    
    def validate_request(self, params: Dict[str, Any]) -> bool:
        """验证请求参数"""
        data_type = params.get('data_type', 'bars')
        
        if data_type in ['bars', 'quote', 'trades', 'prev_close']:
            return 'symbol' in params
        elif data_type == 'ticker_details':
            return 'symbol' in params
        
        return False
    
    async def fetch_data(self, params: Dict[str, Any]) -> Any:
        """获取原始数据"""
        data_type = params.get('data_type', 'bars')
        
        if data_type == 'bars':
            return await self._fetch_bars_data(params)
        elif data_type == 'quote':
            return await self._fetch_quote_data(params)
        elif data_type == 'prev_close':
            return await self._fetch_prev_close(params)
        elif data_type == 'ticker_details':
            return await self._fetch_ticker_details(params)
        else:
            raise ValueError(f"Unsupported data type: {data_type}")
    
    async def _fetch_bars_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取K线数据"""
        symbol = params['symbol']
        multiplier = params.get('multiplier', 1)
        timespan = params.get('timespan', 'day')  # minute, hour, day, week, month, quarter, year
        from_date = params.get('from', (datetime.now() - timedelta(days=30)).strftime('%Y-%m-%d'))
        to_date = params.get('to', datetime.now().strftime('%Y-%m-%d'))
        adjusted = params.get('adjusted', 'true')
        sort = params.get('sort', 'asc')
        limit = params.get('limit', 5000)
        
        api_params = {
            'adjusted': adjusted,
            'sort': sort,
            'limit': limit,
            'apikey': self.config.api_key
        }
        
        url = f"{self.config.base_url}/v2/aggs/ticker/{symbol}/range/{multiplier}/{timespan}/{from_date}/{to_date}?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                if data.get('status') != 'OK':
                    raise Exception(f"Polygon API error: {data.get('status')} - {data.get('error', 'Unknown error')}")
                
                results = data.get('results', [])
                if not results:
                    raise Exception(f"No bar data found for {symbol}")
                
                data_points = []
                for bar in results:
                    data_point = {
                        'timestamp': datetime.fromtimestamp(bar['t'] / 1000).isoformat(),  # Polygon uses milliseconds
                        'open': bar.get('o'),
                        'high': bar.get('h'),
                        'low': bar.get('l'),
                        'close': bar.get('c'),
                        'volume': bar.get('v'),
                        'volume_weighted_price': bar.get('vw'),
                        'number_of_transactions': bar.get('n')
                    }
                    data_points.append(data_point)
                
                return {
                    'symbol': symbol,
                    'data': data_points,
                    'meta': {
                        'currency': 'USD',
                        'timespan': timespan,
                        'multiplier': multiplier,
                        'count': data.get('resultsCount', 0),
                        'adjusted': adjusted,
                        'next_url': data.get('next_url', '')
                    }
                }
    
    async def _fetch_quote_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取最新报价"""
        symbol = params['symbol']
        
        api_params = {'apikey': self.config.api_key}
        url = f"{self.config.base_url}/v2/last/nbbo/{symbol}?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                if data.get('status') != 'OK':
                    raise Exception(f"Polygon API error: {data.get('status')}")
                
                result = data.get('results', {})
                if not result:
                    raise Exception(f"No quote data found for {symbol}")
                
                return {
                    'symbol': symbol,
                    'bid_price': result.get('P'),  # bid price
                    'bid_size': result.get('S'),   # bid size
                    'ask_price': result.get('p'),  # ask price  
                    'ask_size': result.get('s'),   # ask size
                    'exchange': result.get('X'),   # bid exchange
                    'ask_exchange': result.get('x'), # ask exchange
                    'timestamp': result.get('t'),   # timestamp
                    'currency': 'USD',
                    'last_trade_time': datetime.now().isoformat()
                }
    
    async def _fetch_prev_close(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取前一交易日收盘数据"""
        symbol = params['symbol']
        adjusted = params.get('adjusted', 'true')
        
        api_params = {
            'adjusted': adjusted,
            'apikey': self.config.api_key
        }
        
        url = f"{self.config.base_url}/v2/aggs/ticker/{symbol}/prev?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                if data.get('status') != 'OK':
                    raise Exception(f"Polygon API error: {data.get('status')}")
                
                results = data.get('results', [])
                if not results:
                    raise Exception(f"No previous close data found for {symbol}")
                
                result = results[0]
                
                return {
                    'symbol': symbol,
                    'open': result.get('o'),
                    'high': result.get('h'),
                    'low': result.get('l'),
                    'close': result.get('c'),
                    'volume': result.get('v'),
                    'volume_weighted_price': result.get('vw'),
                    'timestamp': result.get('t'),
                    'currency': 'USD',
                    'adjusted': adjusted
                }
    
    async def _fetch_ticker_details(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取股票详细信息"""
        symbol = params['symbol']
        date = params.get('date', datetime.now().strftime('%Y-%m-%d'))
        
        api_params = {'date': date, 'apikey': self.config.api_key}
        url = f"{self.config.base_url}/v3/reference/tickers/{symbol}?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                if data.get('status') != 'OK':
                    raise Exception(f"Polygon API error: {data.get('status')}")
                
                result = data.get('results', {})
                if not result:
                    raise Exception(f"No ticker details found for {symbol}")
                
                return {
                    'symbol': result.get('ticker', symbol),
                    'company_name': result.get('name', ''),
                    'market': result.get('market', ''),
                    'locale': result.get('locale', ''),
                    'primary_exchange': result.get('primary_exchange', ''),
                    'type': result.get('type', ''),
                    'active': result.get('active', True),
                    'currency_name': result.get('currency_name', 'USD'),
                    'cik': result.get('cik', ''),
                    'composite_figi': result.get('composite_figi', ''),
                    'share_class_figi': result.get('share_class_figi', ''),
                    'market_cap': result.get('market_cap'),
                    'phone_number': result.get('phone_number', ''),
                    'address': result.get('address', {}),
                    'description': result.get('description', ''),
                    'sic_code': result.get('sic_code', ''),
                    'sic_description': result.get('sic_description', ''),
                    'ticker_root': result.get('ticker_root', ''),
                    'homepage_url': result.get('homepage_url', ''),
                    'total_employees': result.get('total_employees'),
                    'list_date': result.get('list_date', ''),
                    'branding': result.get('branding', {}),
                    'share_class_shares_outstanding': result.get('share_class_shares_outstanding'),
                    'weighted_shares_outstanding': result.get('weighted_shares_outstanding')
                }
    
    def normalize_data(self, raw_data: Any) -> List[EnhancedPriceData]:
        """标准化数据"""
        if isinstance(raw_data, dict) and 'data' in raw_data:
            # K线数据
            return self._normalize_bars_data(raw_data)
        elif isinstance(raw_data, dict) and ('open' in raw_data or 'close' in raw_data):
            # 单个交易日数据
            return self._normalize_single_bar_data(raw_data)
        
        raise ValueError("Unsupported raw data format")
    
    def _normalize_bars_data(self, raw_data: Dict) -> List[EnhancedPriceData]:
        """标准化K线数据"""
        symbol = raw_data['symbol']
        currency = CurrencyCode.USD
        
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
            
            # Polygon特有字段
            if point.get('volume_weighted_price'):
                price_data.custom_fields['volume_weighted_price'] = point['volume_weighted_price']
            if point.get('number_of_transactions'):
                price_data.custom_fields['number_of_transactions'] = point['number_of_transactions']
            
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
            price_data.ai_metadata.add_semantic_tag("provider", "polygon")
            price_data.ai_metadata.add_semantic_tag("market", "us_equity")
            price_data.ai_metadata.add_semantic_tag("data_quality", "institutional_grade")
            price_data.ai_metadata.add_analysis_hint("liquidity", "high_volume_us_stocks")
            
            normalized_data.append(price_data)
        
        return normalized_data
    
    def _normalize_single_bar_data(self, raw_data: Dict) -> List[EnhancedPriceData]:
        """标准化单个交易日数据"""
        currency = CurrencyCode.USD
        
        price_data = EnhancedPriceData(
            timestamp=datetime.fromtimestamp(raw_data.get('timestamp', 0) / 1000) if raw_data.get('timestamp') else datetime.now(),
            symbol=raw_data['symbol'],
            provider_id=self.provider_id,
            open_value=raw_data.get('open'),
            high_value=raw_data.get('high'),
            low_value=raw_data.get('low'),
            close_value=raw_data.get('close'),
            volume=raw_data.get('volume'),
            currency=currency
        )
        
        # 添加扩展字段
        if raw_data.get('volume_weighted_price'):
            price_data.custom_fields['volume_weighted_price'] = raw_data['volume_weighted_price']
        if raw_data.get('adjusted'):
            price_data.custom_fields['adjusted'] = raw_data['adjusted']
        
        # AI元数据
        price_data.ai_metadata.add_semantic_tag("provider", "polygon")
        price_data.ai_metadata.add_semantic_tag("data_type", "daily_close")
        price_data.ai_metadata.add_semantic_tag("market", "us_equity")
        
        return [price_data]
    
    def _calculate_technical_indicators(self, data_points: List[Dict]) -> TechnicalIndicators:
        """计算技术指标"""
        if len(data_points) < 20:
            return TechnicalIndicators()
        
        closes = [p['close'] for p in data_points if p.get('close')]
        volumes = [p['volume'] for p in data_points if p.get('volume')]
        
        if not closes:
            return TechnicalIndicators()
        
        indicators = TechnicalIndicators()
        
        # 简单移动平均线
        if len(closes) >= 20:
            indicators.sma_20 = sum(closes[-20:]) / 20
        
        # 成交量加权平均价格相关指标
        if volumes and len(closes) == len(volumes):
            total_volume = sum(volumes[-20:]) if len(volumes) >= 20 else sum(volumes)
            if total_volume > 0:
                weighted_price = sum(closes[i] * volumes[i] for i in range(-20, 0) if i + len(closes) >= 0) if len(closes) >= 20 else sum(closes[i] * volumes[i] for i in range(len(closes)))
                indicators.volume_sma = total_volume / min(20, len(volumes))
                # 可以添加更多基于成交量的指标
        
        return indicators
    
    def _calculate_ai_features(self, data_points: List[Dict], current_index: int) -> AIFeatures:
        """计算AI特征"""
        if not data_points:
            return AIFeatures()
        
        closes = [p['close'] for p in data_points if p.get('close')]
        volumes = [p['volume'] for p in data_points if p.get('volume')]
        
        if not closes:
            return AIFeatures()
        
        features = AIFeatures()
        
        # 使用成交量加权价格计算特征
        vwaps = [p.get('volume_weighted_price') for p in data_points if p.get('volume_weighted_price')]
        if vwaps and len(vwaps) >= 2:
            features.momentum_1d = (vwaps[-1] - vwaps[-2]) / vwaps[-2]
        
        # 成交量特征
        if volumes and len(volumes) >= 10:
            avg_volume = sum(volumes[-10:]) / 10
            features.volume_profile = volumes[-1] / avg_volume if avg_volume > 0 else 1.0
        
        return features
    
    def assess_data_quality(self, data: List[EnhancedPriceData]) -> DataQuality:
        """评估数据质量"""
        if not data:
            return DataQuality(
                accuracy_score=0.0,
                completeness_score=0.0,
                timeliness_score=0.0,
                confidence_level=0.5,
                data_sources=["polygon"]
            )
        
        # Polygon数据质量评估（机构级数据）
        total_fields = len(data) * 5
        complete_fields = sum(
            sum(1 for field in [dp.open_value, dp.high_value, dp.low_value, dp.close_value, dp.volume] 
                if field is not None) for dp in data
        )
        completeness_score = complete_fields / total_fields if total_fields > 0 else 0.0
        
        # 时效性评估
        if data:
            latest_time = max(dp.timestamp for dp in data)
            time_diff = datetime.now(timezone.utc) - latest_time.replace(tzinfo=timezone.utc)
            timeliness_score = max(0.0, 1.0 - time_diff.total_seconds() / 3600)  # 1小时内为满分
        else:
            timeliness_score = 0.0
        
        return DataQuality(
            accuracy_score=0.99,  # Polygon机构级数据，准确性极高
            completeness_score=completeness_score,
            timeliness_score=timeliness_score,
            confidence_level=0.98,  # 非常高的可信度
            data_sources=["polygon"],
            last_updated=datetime.now(timezone.utc)
        )
    
    # 实现抽象方法
    async def get_historical_data(self, symbol: str, start_date: str, end_date: str, **kwargs) -> Any:
        """获取历史数据"""
        params = {
            'symbol': symbol,
            'data_type': 'bars',
            'from': start_date,
            'to': end_date,
            'timespan': kwargs.get('timespan', 'day'),
            'multiplier': kwargs.get('multiplier', 1),
            **kwargs
        }
        return await self.get_data(params)
    
    async def get_real_time_quote(self, symbols: List[str], **kwargs) -> Any:
        """获取实时行情"""
        results = []
        for symbol in symbols:
            try:
                # 获取报价数据
                quote_params = {'symbol': symbol, 'data_type': 'quote'}
                quote_result = await self.get_data(quote_params)
                
                # 获取前一交易日数据作为参考
                prev_params = {'symbol': symbol, 'data_type': 'prev_close'}
                prev_result = await self.get_data(prev_params)
                
                # 合并数据
                combined_result = {
                    'symbol': symbol,
                    'quote': quote_result.data[0] if hasattr(quote_result, 'data') else quote_result,
                    'previous_close': prev_result.data[0] if hasattr(prev_result, 'data') else prev_result
                }
                
                results.append(combined_result)
                
            except Exception as e:
                self.logger.warning(f"Failed to get real-time data for {symbol}: {e}")
                results.append({'symbol': symbol, 'error': str(e)})
        
        return results
    
    async def get_company_info(self, symbols: List[str], **kwargs) -> Any:
        """获取公司信息"""
        results = []
        for symbol in symbols:
            params = {
                'symbol': symbol,
                'data_type': 'ticker_details',
                **kwargs
            }
            result = await self.get_data(params)
            results.append(result)
        return results
    
    async def screen_stocks(self, criteria: Dict[str, Any], **kwargs) -> Any:
        """股票筛选（Polygon提供有限的筛选功能）"""
        return {
            'message': 'Polygon provides limited stock screening capabilities',
            'supported_functions': ['real_time_quote', 'historical_data', 'company_info', 'market_data']
        }
    
    async def get_supported_symbols(self, category: DataCategory) -> List[str]:
        """获取支持的标的列表"""
        if category == DataCategory.EQUITY:
            return [
                # 美股主要股票
                'AAPL', 'GOOGL', 'GOOG', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA', 'NFLX',
                'JPM', 'JNJ', 'V', 'PG', 'UNH', 'HD', 'MA', 'DIS', 'ADBE', 'CRM',
                'WMT', 'BAC', 'KO', 'PFE', 'INTC', 'VZ', 'T', 'XOM', 'CVX', 'ABT'
            ]
        elif category == DataCategory.FOREX:
            return ['C:EURUSD', 'C:GBPUSD', 'C:USDJPY', 'C:USDCAD', 'C:AUDUSD']
        elif category == DataCategory.CRYPTO:
            return ['X:BTCUSD', 'X:ETHUSD', 'X:ADAUSD', 'X:DOGUSD']
        else:
            return []