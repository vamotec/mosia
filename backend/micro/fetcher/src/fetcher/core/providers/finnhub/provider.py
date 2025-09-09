"""
Finnhub Provider Implementation
Finnhub数据提供商实现 - 全球股票和新闻数据
"""

from datetime import datetime, timezone
from typing import Any, Dict, List
from urllib.parse import urlencode

import aiohttp
import pandas as pd

from fetcher.config.logging import get_logger
from fetcher.core.models.base import EnhancedPriceData, TechnicalIndicators, AIFeatures, CurrencyCode
from fetcher.core.providers.base import EquityProvider, NewsProvider, DataCategory, DataQuality

logger = get_logger(__name__)

class FinnhubProvider(EquityProvider, NewsProvider):
    """Finnhub数据提供商 - 全球股票和新闻数据"""
    def __init__(self, cache_enabled: bool = True, cache_ttl: int = 300,
                 api_version: str = "v8", **kwargs):
        kwargs.setdefault('provider_id', 'finnhub')
        kwargs.setdefault('provider_name', 'Finnhub')
        kwargs.setdefault('class_path', 'fetcher.core.providers.finnhub.provider.FinnhubProvider')
        kwargs.setdefault('base_url', 'https://finnhub.io/api/v1')
        kwargs.setdefault('supported_categories', ['equity', 'crypto', 'forex'])
        kwargs.setdefault('supported_regions', ['US', 'EU', 'GLOBAL'])
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
            params = {'symbol': 'AAPL', 'token': self.config.api_key}
            url = f"{self.config.base_url}/api/v1/quote?" + urlencode(params)
            
            async with aiohttp.ClientSession() as session:
                async with session.get(url, timeout=self.cache_ttl) as response:
                    if response.status == 200:
                        data = await response.json()
                        return 'c' in data  # 'c' 是current price字段
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
        data_type = params.get('data_type', 'quote')
        
        if data_type in ['quote', 'candle', 'company_profile']:
            return 'symbol' in params
        elif data_type == 'news':
            return True  # 新闻可以无特定参数
        
        return False
    
    async def fetch_data(self, params: Dict[str, Any]) -> Any:
        """获取原始数据"""
        data_type = params.get('data_type', 'quote')
        
        if data_type == 'quote':
            return await self._fetch_quote_data(params)
        elif data_type == 'candle':
            return await self._fetch_candle_data(params)
        elif data_type == 'company_profile':
            return await self._fetch_company_profile(params)
        elif data_type == 'news':
            return await self._fetch_news_data(params)
        else:
            raise ValueError(f"Unsupported data type: {data_type}")
    
    async def _fetch_quote_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取实时报价"""
        symbol = params['symbol']
        
        api_params = {'symbol': symbol, 'token': self.config.api_key}
        url = f"{self.config.base_url}/api/v1/quote?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.cache_ttl) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                # Finnhub返回格式：{c, h, l, o, pc, t}
                if 'c' not in data:
                    raise Exception(f"No quote data found for {symbol}")
                
                return {
                    'symbol': symbol,
                    'current_price': data.get('c', 0),      # current price
                    'high': data.get('h', 0),               # high price of the day
                    'low': data.get('l', 0),                # low price of the day
                    'open': data.get('o', 0),               # open price of the day
                    'previous_close': data.get('pc', 0),    # previous close price
                    'timestamp': data.get('t', 0),          # timestamp
                    'change': data.get('c', 0) - data.get('pc', 0) if data.get('c') and data.get('pc') else 0,
                    'change_percent': ((data.get('c', 0) - data.get('pc', 0)) / data.get('pc', 1)) * 100 if data.get('pc') else 0,
                    'currency': 'USD',
                    'last_trade_time': datetime.now().isoformat()
                }
    
    async def _fetch_candle_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取K线数据"""
        symbol = params['symbol']
        resolution = params.get('resolution', 'D')  # D, W, M, 1, 5, 15, 30, 60
        from_timestamp = params.get('from')
        to_timestamp = params.get('to')
        
        if not from_timestamp or not to_timestamp:
            # 默认获取过去30天的数据
            to_timestamp = int(datetime.now().timestamp())
            from_timestamp = to_timestamp - 30 * 24 * 3600
        
        api_params = {
            'symbol': symbol,
            'resolution': resolution,
            'from': from_timestamp,
            'to': to_timestamp,
            'token': self.config.api_key
        }
        
        url = f"{self.config.base_url}/api/v1/stock/candle?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                if data.get('s') != 'ok':
                    raise Exception(f"Finnhub API error: {data.get('s', 'Unknown error')}")
                
                # Finnhub返回格式：{c, h, l, o, t, v, s}
                timestamps = data.get('t', [])
                closes = data.get('c', [])
                highs = data.get('h', [])
                lows = data.get('l', [])
                opens = data.get('o', [])
                volumes = data.get('v', [])
                
                data_points = []
                for i in range(len(timestamps)):
                    data_point = {
                        'timestamp': datetime.fromtimestamp(timestamps[i]).isoformat(),
                        'open': opens[i] if i < len(opens) else None,
                        'high': highs[i] if i < len(highs) else None,
                        'low': lows[i] if i < len(lows) else None,
                        'close': closes[i] if i < len(closes) else None,
                        'volume': volumes[i] if i < len(volumes) else None
                    }
                    data_points.append(data_point)
                
                return {
                    'symbol': symbol,
                    'data': data_points,
                    'meta': {
                        'currency': 'USD',
                        'resolution': resolution,
                        'status': data.get('s')
                    }
                }
    
    async def _fetch_company_profile(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取公司基础信息"""
        symbol = params['symbol']
        
        api_params = {'symbol': symbol, 'token': self.config.api_key}
        url = f"{self.config.base_url}/api/v1/stock/profile2?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.cache_ttl) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                if not data or 'name' not in data:
                    raise Exception(f"No company profile found for {symbol}")
                
                return {
                    'symbol': symbol,
                    'company_name': data.get('name', ''),
                    'country': data.get('country', ''),
                    'currency': data.get('currency', 'USD'),
                    'exchange': data.get('exchange', ''),
                    'ipo_date': data.get('ipo', ''),
                    'market_cap': data.get('marketCapitalization', 0),
                    'shares_outstanding': data.get('shareOutstanding', 0),
                    'industry': data.get('finnhubIndustry', ''),
                    'logo': data.get('logo', ''),
                    'phone': data.get('phone', ''),
                    'website': data.get('weburl', ''),
                    'ticker': data.get('ticker', symbol)
                }
    
    async def _fetch_news_data(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取新闻数据"""
        category = params.get('category', 'general')
        min_id = params.get('min_id', 0)
        
        api_params = {
            'category': category,
            'min_id': min_id,
            'token': self.config.api_key
        }
        
        url = f"{self.config.base_url}/api/v1/news?" + urlencode(api_params)
        
        async with aiohttp.ClientSession() as session:
            async with session.get(url, timeout=self.config.timeout) as response:
                if response.status != 200:
                    raise Exception(f"HTTP {response.status}: {await response.text()}")
                
                data = await response.json()
                
                news_list = []
                for article in data[:50]:  # 限制返回数量
                    news_item = {
                        'id': article.get('id', ''),
                        'headline': article.get('headline', ''),
                        'summary': article.get('summary', ''),
                        'source': article.get('source', ''),
                        'url': article.get('url', ''),
                        'datetime': article.get('datetime', 0),
                        'image': article.get('image', ''),
                        'category': category,
                        'language': 'en',
                        'related_symbols': article.get('related', ''),
                        'publish_time': datetime.fromtimestamp(article.get('datetime', 0)).isoformat() if article.get('datetime') else ''
                    }
                    news_list.append(news_item)
                
                return {'news': news_list}
    
    def normalize_data(self, raw_data: Any) -> List[EnhancedPriceData]:
        """标准化数据"""
        if isinstance(raw_data, dict):
            if 'data' in raw_data:
                # K线数据
                return self._normalize_candle_data(raw_data)
            elif 'current_price' in raw_data:
                # 行情数据
                return self._normalize_quote_data(raw_data)
        
        raise ValueError("Unsupported raw data format")
    
    def _normalize_candle_data(self, raw_data: Dict) -> List[EnhancedPriceData]:
        """标准化K线数据"""
        symbol = raw_data['symbol']
        currency_str = raw_data.get('meta', {}).get('currency', 'USD')
        
        try:
            currency = CurrencyCode(currency_str)
        except ValueError:
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
            price_data.ai_metadata.add_semantic_tag("provider", "finnhub")
            price_data.ai_metadata.add_semantic_tag("market", "global")
            price_data.ai_metadata.add_semantic_tag("currency", currency_str)
            price_data.ai_metadata.add_analysis_hint("data_quality", "professional_realtime")
            
            normalized_data.append(price_data)
        
        return normalized_data
    
    def _normalize_quote_data(self, raw_data: Dict) -> List[EnhancedPriceData]:
        """标准化行情数据"""
        currency = CurrencyCode.USD
        
        price_data = EnhancedPriceData(
            timestamp=datetime.now(),
            symbol=raw_data['symbol'],
            provider_id=self.provider_id,
            open_value=raw_data.get('open'),
            high_value=raw_data.get('high'),
            low_value=raw_data.get('low'),
            close_value=raw_data.get('current_price'),
            currency=currency,
            change=raw_data.get('change'),
            change_percent=raw_data.get('change_percent')
        )
        
        # 添加扩展字段
        price_data.custom_fields.update({
            'previous_close': raw_data.get('previous_close'),
            'timestamp': raw_data.get('timestamp')
        })
        
        # AI元数据
        price_data.ai_metadata.add_semantic_tag("provider", "finnhub")
        price_data.ai_metadata.add_semantic_tag("data_type", "realtime")
        price_data.ai_metadata.add_semantic_tag("market", "global")
        
        return [price_data]
    
    def _calculate_technical_indicators(self, data_points: List[Dict]) -> TechnicalIndicators:
        """计算技术指标"""
        if len(data_points) < 20:
            return TechnicalIndicators()
        
        closes = [p['close'] for p in data_points if p.get('close')]
        if not closes:
            return TechnicalIndicators()
        
        indicators = TechnicalIndicators()
        
        # 简单移动平均线
        if len(closes) >= 20:
            indicators.sma_20 = sum(closes[-20:]) / 20
        
        # RSI
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
        
        # 基本波动率计算
        if len(closes) >= 20:
            returns = [(closes[i] - closes[i-1]) / closes[i-1] for i in range(1, len(closes))]
            variance = sum(r ** 2 for r in returns) / len(returns)
            features.volatility = (variance ** 0.5) * (252 ** 0.5)  # 年化波动率
        
        return features
    
    def assess_data_quality(self, data: List[EnhancedPriceData]) -> DataQuality:
        """评估数据质量"""
        if not data:
            return DataQuality(
                accuracy_score=0.0,
                completeness_score=0.0,
                timeliness_score=0.0,
                confidence_level=0.5,
                data_sources=["finnhub"]
            )
        
        # Finnhub数据质量评估
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
            accuracy_score=0.95,  # Finnhub专业数据，准确性高
            completeness_score=completeness_score,
            timeliness_score=timeliness_score,
            confidence_level=0.92,
            data_sources=["finnhub"],
            last_updated=datetime.now(timezone.utc)
        )
    
    # 实现抽象方法
    async def get_historical_data(self, symbol: str, start_date: str, end_date: str, **kwargs) -> Any:
        """获取历史数据"""
        # 转换日期为时间戳
        from_ts = int(datetime.strptime(start_date, '%Y-%m-%d').timestamp())
        to_ts = int(datetime.strptime(end_date, '%Y-%m-%d').timestamp())
        
        params = {
            'symbol': symbol,
            'data_type': 'candle',
            'resolution': kwargs.get('resolution', 'D'),
            'from': from_ts,
            'to': to_ts,
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
        """获取公司信息"""
        results = []
        for symbol in symbols:
            params = {
                'symbol': symbol,
                'data_type': 'company_profile',
                **kwargs
            }
            result = await self.get_data(params)
            results.append(result)
        return results
    
    async def screen_stocks(self, criteria: Dict[str, Any], **kwargs) -> Any:
        """股票筛选（Finnhub不直接支持）"""
        return {
            'message': 'Finnhub does not support stock screening directly',
            'supported_functions': ['real_time_quote', 'historical_data', 'company_info', 'news']
        }
    
    async def get_news(self, query: str, limit: int, **kwargs) -> Any:
        """获取新闻"""
        params = {
            'data_type': 'news',
            'category': kwargs.get('category', 'general'),
            'limit': limit,
            **kwargs
        }
        return await self.get_data(params)
    
    async def get_news_by_symbol(self, symbols: List[str], **kwargs) -> Any:
        """根据股票代码获取相关新闻"""
        results = []
        for symbol in symbols:
            try:
                api_params = {
                    'symbol': symbol,
                    'from': (datetime.now() - pd.Timedelta(days=7)).strftime('%Y-%m-%d'),
                    'to': datetime.now().strftime('%Y-%m-%d'),
                    'token': self.config.api_key
                }
                
                url = f"{self.config.base_url}/api/v1/company-news?" + urlencode(api_params)
                
                async with aiohttp.ClientSession() as session:
                    async with session.get(url, timeout=self.config.timeout) as response:
                        if response.status == 200:
                            data = await response.json()
                            news_list = []
                            
                            for article in data[:20]:  # 限制数量
                                news_item = {
                                    'symbol': symbol,
                                    'headline': article.get('headline', ''),
                                    'summary': article.get('summary', ''),
                                    'source': article.get('source', ''),
                                    'url': article.get('url', ''),
                                    'datetime': article.get('datetime', 0),
                                    'image': article.get('image', ''),
                                    'category': article.get('category', ''),
                                    'language': 'en',
                                    'publish_time': datetime.fromtimestamp(article.get('datetime', 0)).isoformat() if article.get('datetime') else ''
                                }
                                news_list.append(news_item)
                            
                            results.append({'symbol': symbol, 'news': news_list})
                        else:
                            results.append({'symbol': symbol, 'error': f'HTTP {response.status}'})
            except Exception as e:
                results.append({'symbol': symbol, 'error': str(e)})
        
        return results
    
    async def analyze_sentiment(self, text: str, **kwargs) -> Any:
        """情感分析（Finnhub提供新闻情感分析）"""
        # 简单实现，实际可以调用Finnhub的情感分析API
        return {
            'sentiment': 'neutral',
            'score': 0.0,
            'confidence': 0.5
        }
    
    async def get_supported_symbols(self, category: DataCategory) -> List[str]:
        """获取支持的标的列表"""
        if category == DataCategory.EQUITY:
            return [
                # 美股主要股票
                'AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA', 'NFLX',
                'JPM', 'JNJ', 'V', 'PG', 'UNH', 'HD', 'MA', 'DIS', 'ADBE', 'CRM',
                # 欧股
                'ASML', 'SAP', 'NESN.SW', 'ROCHE.SW', 'MC.PA',
                # 亚股
                'TSM', 'BABA', 'TCEHY'
            ]
        else:
            return []