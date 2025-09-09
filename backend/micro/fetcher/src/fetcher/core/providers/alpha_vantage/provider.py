"""
Alpha Vantage Provider Implementation
Alpha Vantage数据提供商实现 - 全球股票、外汇、加密货币数据
"""

from datetime import datetime, timezone
from typing import Any, Dict, List
from urllib.parse import urlencode

import aiohttp

from fetcher.config.logging import get_logger
from fetcher.core.models.base import EnhancedPriceData, TechnicalIndicators, AIFeatures, CurrencyCode
from fetcher.core.providers.base import EquityProvider, DataCategory, DataQuality

logger = get_logger(__name__)

class AlphaVantageProvider(EquityProvider):
    """Alpha Vantage数据提供商 - 全球股票、外汇、加密货币数据"""
    def __init__(self, cache_enabled: bool = True, cache_ttl: int = 300,
                 api_version: str = "v8", **kwargs):
        kwargs.setdefault('provider_id', 'alpha_vantage')
        kwargs.setdefault('provider_name', 'Alpha Vantage')
        kwargs.setdefault('class_path', 'fetcher.core.providers.alpha_vantage.provider.AlphaVantageProvider')
        kwargs.setdefault('base_url', 'https://www.alphavantage.co')
        kwargs.setdefault('supported_categories', ['equity', 'forex', 'crypto'])
        kwargs.setdefault('supported_regions', ['US', 'GLOBAL'])
        super().__init__(**kwargs)
        self.cache_enabled = cache_enabled
        self.cache_ttl = cache_ttl
        self.api_version = api_version

    async def initialize(self):
        """初始化缓存等资源"""
        if self.cache_enabled:
            logger.info(f"AlphaVantage 提供商启用缓存，TTL: {self.cache_ttl}秒")
    
    async def validate_credentials(self) -> bool:
        """验证API凭证"""
        try:
            params = {
                'function': 'GLOBAL_QUOTE',
                'symbol': 'AAPL',
                'apikey': self.config.api_key
            }
            
            url = f"{self.config.base_url}/query?" + urlencode(params)
            
            async with aiohttp.ClientSession() as session:
                async with session.get(url, timeout=self.config.timeout) as response:
                    if response.status == 200:
                        data = await response.json()
                        # 检查是否包含有效数据
                        return 'Global Quote' in data and '01. symbol' in data.get('Global Quote', {})
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
        data_type = params.get('data_type', 'historical')
        
        if data_type in ['historical', 'quote', 'intraday']:
            return 'symbol' in params
        elif data_type == 'forex':
            return 'from_currency' in params and 'to_currency' in params
        elif data_type == 'crypto':
            return 'symbol' in params
        
        return False
    
    async def fetch_data(self, params: Dict[str, Any]) -> Any:
        """获取原始数据"""
        data_type = params.get('data_type', 'historical')
        
        if data_type == 'historical':
            return await self._fetch_historical_data(params)
        elif data_type == 'intraday':
            return await self._fetch_intraday_data(params)
        elif data_type == 'quote':
            return await self._fetch_quote_data(params)
        elif data_type == 'forex':
            return await self._fetch_forex_data(params)
        elif data_type == 'crypto':
            return await self._fetch_crypto_data(params)
        else:
            raise ValueError(f"Unsupported data type: {data_type}")
    
    async def _fetch_historical_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取历史数据"""
        symbol = params['symbol']
        output_size = params.get('output_size', 'compact')  # compact or full
        
        api_params = {
            'function': 'TIME_SERIES_DAILY_ADJUSTED',
            'symbol': symbol,
            'outputsize': output_size,
            'apikey': self.config.api_key
        }
        
        url = f"{self.config.base_url}/query?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                # 检查错误
                if 'Error Message' in data:
                    raise Exception(f"Alpha Vantage API error: {data['Error Message']}")
                if 'Note' in data:
                    raise Exception(f"Alpha Vantage API limit: {data['Note']}")
                
                # 提取时间序列数据
                time_series_key = 'Time Series (Daily)'
                if time_series_key not in data:
                    raise Exception(f"No time series data found for {symbol}")
                
                time_series = data[time_series_key]
                metadata = data.get('Meta Data', {})
                
                # 转换数据格式
                data_points = []
                for date_str, values in time_series.items():
                    data_point = {
                        'timestamp': datetime.strptime(date_str, '%Y-%m-%d').isoformat(),
                        'open': float(values['1. open']),
                        'high': float(values['2. high']),
                        'low': float(values['3. low']),
                        'close': float(values['4. close']),
                        'adjusted_close': float(values['5. adjusted close']),
                        'volume': int(values['6. volume']),
                        'dividend_amount': float(values['7. dividend amount']),
                        'split_coefficient': float(values['8. split coefficient'])
                    }
                    data_points.append(data_point)
                
                # 按时间排序（最新的在后面）
                data_points.sort(key=lambda x: x['timestamp'])
                
                return {
                    'symbol': symbol,
                    'data': data_points,
                    'meta': {
                        'currency': 'USD',  # Alpha Vantage主要是美股，默认USD
                        'exchange': metadata.get('4. Output Size', ''),
                        'last_refreshed': metadata.get('3. Last Refreshed', ''),
                        'time_zone': metadata.get('5. Time Zone', 'US/Eastern')
                    }
                }
    
    async def _fetch_intraday_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取日内数据"""
        symbol = params['symbol']
        interval = params.get('interval', '5min')  # 1min, 5min, 15min, 30min, 60min
        output_size = params.get('output_size', 'compact')
        
        api_params = {
            'function': 'TIME_SERIES_INTRADAY',
            'symbol': symbol,
            'interval': interval,
            'outputsize': output_size,
            'apikey': self.config.api_key
        }
        
        url = f"{self.config.base_url}/query?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                # 检查错误
                if 'Error Message' in data:
                    raise Exception(f"Alpha Vantage API error: {data['Error Message']}")
                if 'Note' in data:
                    raise Exception(f"Alpha Vantage API limit: {data['Note']}")
                
                # 提取时间序列数据
                time_series_key = f'Time Series ({interval})'
                if time_series_key not in data:
                    raise Exception(f"No intraday data found for {symbol}")
                
                time_series = data[time_series_key]
                metadata = data.get('Meta Data', {})
                
                # 转换数据格式
                data_points = []
                for datetime_str, values in time_series.items():
                    data_point = {
                        'timestamp': datetime.strptime(datetime_str, '%Y-%m-%d %H:%M:%S').isoformat(),
                        'open': float(values['1. open']),
                        'high': float(values['2. high']),
                        'low': float(values['3. low']),
                        'close': float(values['4. close']),
                        'volume': int(values['5. volume'])
                    }
                    data_points.append(data_point)
                
                # 按时间排序
                data_points.sort(key=lambda x: x['timestamp'])
                
                return {
                    'symbol': symbol,
                    'data': data_points,
                    'meta': {
                        'currency': 'USD',
                        'interval': interval,
                        'last_refreshed': metadata.get('3. Last Refreshed', ''),
                        'time_zone': metadata.get('4. Time Zone', 'US/Eastern')
                    }
                }
    
    async def _fetch_quote_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取实时报价"""
        symbol = params['symbol']
        
        api_params = {
            'function': 'GLOBAL_QUOTE',
            'symbol': symbol,
            'apikey': self.config.api_key
        }
        
        url = f"{self.config.base_url}/query?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                # 检查错误
                if 'Error Message' in data:
                    raise Exception(f"Alpha Vantage API error: {data['Error Message']}")
                if 'Note' in data:
                    raise Exception(f"Alpha Vantage API limit: {data['Note']}")
                
                if 'Global Quote' not in data:
                    raise Exception(f"No quote data found for {symbol}")
                
                quote = data['Global Quote']
                
                return {
                    'symbol': quote['01. symbol'],
                    'open': float(quote['02. open']),
                    'high': float(quote['03. high']),
                    'low': float(quote['04. low']),
                    'current_price': float(quote['05. price']),
                    'volume': int(quote['06. volume']),
                    'latest_trading_day': quote['07. latest trading day'],
                    'previous_close': float(quote['08. previous close']),
                    'change': float(quote['09. change']),
                    'change_percent': float(quote['10. change percent'].replace('%', '')),
                    'currency': 'USD',
                    'last_trade_time': datetime.now().isoformat()
                }
    
    async def _fetch_forex_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取外汇数据"""
        from_currency = params['from_currency']
        to_currency = params['to_currency']
        
        api_params = {
            'function': 'FX_DAILY',
            'from_symbol': from_currency,
            'to_symbol': to_currency,
            'apikey': self.config.api_key
        }
        
        url = f"{self.config.base_url}/query?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                # 检查错误
                if 'Error Message' in data:
                    raise Exception(f"Alpha Vantage API error: {data['Error Message']}")
                
                time_series_key = f'Time Series FX (Daily)'
                if time_series_key not in data:
                    raise Exception(f"No forex data found for {from_currency}/{to_currency}")
                
                time_series = data[time_series_key]
                
                # 转换数据格式
                data_points = []
                for date_str, values in time_series.items():
                    data_point = {
                        'timestamp': datetime.strptime(date_str, '%Y-%m-%d').isoformat(),
                        'open': float(values['1. open']),
                        'high': float(values['2. high']),
                        'low': float(values['3. low']),
                        'close': float(values['4. close'])
                    }
                    data_points.append(data_point)
                
                data_points.sort(key=lambda x: x['timestamp'])
                
                return {
                    'symbol': f"{from_currency}/{to_currency}",
                    'data': data_points,
                    'meta': {
                        'from_currency': from_currency,
                        'to_currency': to_currency,
                        'currency': to_currency
                    }
                }
    
    async def _fetch_crypto_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取加密货币数据"""
        symbol = params['symbol']
        market = params.get('market', 'USD')
        
        api_params = {
            'function': 'DIGITAL_CURRENCY_DAILY',
            'symbol': symbol,
            'market': market,
            'apikey': self.config.api_key
        }
        
        url = f"{self.config.base_url}/query?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                # 检查错误
                if 'Error Message' in data:
                    raise Exception(f"Alpha Vantage API error: {data['Error Message']}")
                
                time_series_key = f'Time Series (Digital Currency Daily)'
                if time_series_key not in data:
                    raise Exception(f"No crypto data found for {symbol}")
                
                time_series = data[time_series_key]
                
                # 转换数据格式
                data_points = []
                for date_str, values in time_series.items():
                    data_point = {
                        'timestamp': datetime.strptime(date_str, '%Y-%m-%d').isoformat(),
                        'open': float(values[f'1a. open ({market})']),
                        'high': float(values[f'2a. high ({market})']),
                        'low': float(values[f'3a. low ({market})']),
                        'close': float(values[f'4a. close ({market})']),
                        'volume': float(values['5. volume']),
                        'market_cap': float(values[f'6. market cap ({market})'])
                    }
                    data_points.append(data_point)
                
                data_points.sort(key=lambda x: x['timestamp'])
                
                return {
                    'symbol': f"{symbol}-{market}",
                    'data': data_points,
                    'meta': {
                        'digital_currency_code': symbol,
                        'market_code': market,
                        'currency': market
                    }
                }
    
    def normalize_data(self, raw_data: Any) -> List[EnhancedPriceData]:
        """标准化数据"""
        if not isinstance(raw_data, dict) or 'data' not in raw_data:
            raise ValueError("Invalid raw data format")
        
        symbol = raw_data['symbol']
        currency_str = raw_data.get('meta', {}).get('currency', 'USD')
        
        try:
            currency = CurrencyCode(currency_str)
        except ValueError:
            currency = CurrencyCode.USD
        
        normalized_data = []
        data_points = raw_data['data']
        
        for i, point in enumerate(data_points):
            # 创建基础价格数据
            price_data = EnhancedPriceData(
                timestamp=datetime.fromisoformat(point['timestamp']),
                symbol=symbol,
                provider_id=self.provider_id,
                open_value=point.get('open'),
                high_value=point.get('high'),
                low_value=point.get('low'),
                close_value=point.get('close'),
                volume=point.get('volume'),
                currency=currency,
                dividend_amount=point.get('dividend_amount'),
                split_ratio=point.get('split_coefficient', 1.0)
            )
            
            # Alpha Vantage特有字段
            if point.get('adjusted_close'):
                price_data.custom_fields['adjusted_close'] = point['adjusted_close']
            if point.get('market_cap'):
                price_data.custom_fields['market_cap'] = point['market_cap']
            
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
            price_data.ai_metadata.add_semantic_tag("provider", "alpha_vantage")
            price_data.ai_metadata.add_semantic_tag("market", "global")
            price_data.ai_metadata.add_semantic_tag("currency", currency_str)
            price_data.ai_metadata.add_analysis_hint("data_quality", "high_accuracy_professional_grade")
            
            normalized_data.append(price_data)
        
        return normalized_data
    
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
        if len(closes) >= 50:
            indicators.sma_50 = sum(closes[-50:]) / 50
        
        # RSI计算
        if len(closes) >= 14:
            indicators.rsi = self._calculate_rsi(closes, 14)
        
        return indicators
    
    def _calculate_rsi(self, prices: List[float], period: int = 14) -> float:
        """计算RSI"""
        if len(prices) < period + 1:
            return 50.0
        
        gains = []
        losses = []
        
        for i in range(1, len(prices)):
            change = prices[i] - prices[i-1]
            if change > 0:
                gains.append(change)
                losses.append(0)
            else:
                gains.append(0)
                losses.append(-change)
        
        if len(gains) < period:
            return 50.0
        
        avg_gain = sum(gains[-period:]) / period
        avg_loss = sum(losses[-period:]) / period
        
        if avg_loss == 0:
            return 100.0
        
        rs = avg_gain / avg_loss
        rsi = 100 - (100 / (1 + rs))
        
        return rsi
    
    def _calculate_ai_features(self, data_points: List[Dict], current_index: int) -> AIFeatures:
        """计算AI特征"""
        if not data_points:
            return AIFeatures()
        
        closes = [p['close'] for p in data_points if p.get('close')]
        
        if not closes:
            return AIFeatures()
        
        features = AIFeatures()
        
        # 计算波动率
        if len(closes) >= 20:
            returns = [(closes[i] - closes[i-1]) / closes[i-1] for i in range(1, len(closes))]
            variance = sum(r ** 2 for r in returns) / len(returns)
            features.volatility = (variance ** 0.5) * (252 ** 0.5)  # 年化波动率
        
        # 动量计算
        if len(closes) >= 2:
            features.momentum_1d = (closes[-1] - closes[-2]) / closes[-2]
        if len(closes) >= 6:
            features.momentum_5d = (closes[-1] - closes[-6]) / closes[-6]
        
        return features
    
    def assess_data_quality(self, data: List[EnhancedPriceData]) -> DataQuality:
        """评估数据质量"""
        if not data:
            return DataQuality(
                accuracy_score=0.0,
                completeness_score=0.0,
                timeliness_score=0.0,
                confidence_level=0.5,
                data_sources=["alpha_vantage"]
            )
        
        # Alpha Vantage数据质量通常很高
        total_fields = len(data) * 5
        complete_fields = sum(
            sum(1 for field in [dp.open_value, dp.high_value, dp.low_value, dp.close_value, dp.volume] 
                if field is not None) for dp in data
        )
        completeness_score = complete_fields / total_fields if total_fields > 0 else 0.0
        
        # 时效性评估
        if data:
            latest_time = max(dp.timestamp for dp in data)
            time_diff = datetime.now(timezone.utc) - latest_time
            timeliness_score = max(0.0, 1.0 - time_diff.total_seconds() / 86400)  # 24小时内为满分
        else:
            timeliness_score = 0.0
        
        return DataQuality(
            accuracy_score=0.98,  # Alpha Vantage专业级数据，准确性很高
            completeness_score=completeness_score,
            timeliness_score=timeliness_score,
            confidence_level=0.95,  # 高可信度
            data_sources=["alpha_vantage"],
            last_updated=datetime.now(timezone.utc)
        )
    
    # 实现抽象方法
    async def get_historical_data(self, symbol: str, start_date: str, end_date: str, **kwargs) -> Any:
        """获取历史数据"""
        params = {
            'symbol': symbol,
            'data_type': 'historical',
            'output_size': kwargs.get('output_size', 'compact'),
            **kwargs
        }
        return await self.get_data(params)
    
    async def get_real_time_quote(self, symbols: List[str], **kwargs) -> Any:
        """获取实时行情"""
        results = []
        for symbol in symbols:
            params = {
                'symbol': symbol,
                'data_type': 'quote',
                **kwargs
            }
            result = await self.get_data(params)
            results.append(result)
        return results
    
    async def get_company_info(self, symbols: List[str], **kwargs) -> Any:
        """获取公司信息（Alpha Vantage需要单独的API调用）"""
        results = []
        for symbol in symbols:
            # Alpha Vantage的公司基础信息需要使用OVERVIEW函数
            try:
                api_params = {
                    'function': 'OVERVIEW',
                    'symbol': symbol,
                    'apikey': self.config.api_key
                }
                
                url = f"{self.config.base_url}/query?" + urlencode(api_params)
                
                async with aiohttp.ClientSession() as session:
                    async with session.get(url, timeout=self.config.timeout) as response:
                        if response.status == 200:
                            data = await response.json()
                            
                            if 'Symbol' in data:
                                company_info = {
                                    'symbol': data.get('Symbol', symbol),
                                    'company_name': data.get('Name', ''),
                                    'business_summary': data.get('Description', ''),
                                    'industry': data.get('Industry', ''),
                                    'sector': data.get('Sector', ''),
                                    'country': data.get('Country', ''),
                                    'currency': data.get('Currency', 'USD'),
                                    'exchange': data.get('Exchange', ''),
                                    'market_cap': int(data.get('MarketCapitalization', 0)) if data.get('MarketCapitalization') else None,
                                    'pe_ratio': float(data.get('PERatio', 0)) if data.get('PERatio') != 'None' else None,
                                    'pb_ratio': float(data.get('PriceToBookRatio', 0)) if data.get('PriceToBookRatio') != 'None' else None,
                                    'dividend_yield': float(data.get('DividendYield', 0)) if data.get('DividendYield') != 'None' else None,
                                    'beta': float(data.get('Beta', 0)) if data.get('Beta') != 'None' else None
                                }
                                results.append(company_info)
                            else:
                                results.append({'symbol': symbol, 'error': 'No company data found'})
                        else:
                            results.append({'symbol': symbol, 'error': f'HTTP {response.status}'})
            except Exception as e:
                results.append({'symbol': symbol, 'error': str(e)})
                
        return results
    
    async def screen_stocks(self, criteria: Dict[str, Any], **kwargs) -> Any:
        """股票筛选（Alpha Vantage API不直接支持筛选，返回基础实现）"""
        return {
            'message': 'Alpha Vantage does not support stock screening directly',
            'supported_functions': ['historical_data', 'real_time_quote', 'company_info', 'forex', 'crypto']
        }
    
    async def get_supported_symbols(self, category: DataCategory) -> List[str]:
        """获取支持的标的列表"""
        if category == DataCategory.EQUITY:
            return [
                # 美股主要股票
                'AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA', 'NFLX',
                'JPM', 'JNJ', 'V', 'PG', 'UNH', 'HD', 'MA', 'DIS', 'ADBE', 'CRM'
            ]
        elif category == DataCategory.FOREX:
            return [
                'EUR/USD', 'GBP/USD', 'USD/JPY', 'USD/CHF', 'AUD/USD', 'USD/CAD',
                'NZD/USD', 'EUR/GBP', 'EUR/JPY', 'GBP/JPY'
            ]
        elif category == DataCategory.CRYPTO:
            return [
                'BTC', 'ETH', 'BNB', 'XRP', 'ADA', 'DOGE', 'MATIC', 'SOL', 'DOT', 'LTC'
            ]
        else:
            return []