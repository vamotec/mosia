"""
Yahoo Finance Provider Implementation
Yahoo Finance数据提供商实现
"""

from datetime import datetime, timezone
from typing import Any, Dict, List

import aiohttp
import pandas as pd
import yfinance as yf

from fetcher.config.logging import get_logger
from fetcher.core.models.base import EnhancedPriceData, CurrencyCode, TechnicalIndicators, AIFeatures
from fetcher.core.providers.base import EquityProvider, DataCategory, DataQuality

logger = get_logger(__name__)

class YahooFinanceProvider(EquityProvider):
    """Yahoo Finance数据提供商"""
    def __init__(self, cache_enabled: bool = True, cache_ttl: int = 300,
                 api_version: str = "v8", **kwargs):
        kwargs.setdefault('provider_id', 'yahoo')
        kwargs.setdefault('provider_name', 'Yahoo Finance')
        kwargs.setdefault('class_path', 'fetcher.core.providers.yahoo.provider.YahooFinanceProvider')
        kwargs.setdefault('base_url', 'https://query1.finance.yahoo.com')
        kwargs.setdefault('supported_categories', ['equity', 'crypto', 'forex'])
        kwargs.setdefault('supported_regions', ['US', 'HK', 'CN'])
        super().__init__(**kwargs)
        self.cache_enabled = cache_enabled
        self.cache_ttl = cache_ttl
        self.api_version = api_version

    async def initialize(self):
        """初始化缓存等资源"""
        if self.cache_enabled:
            logger.info(f"Yahoo Finance 提供商启用缓存，TTL: {self.cache_ttl}秒")

    async def validate_credentials(self) -> bool:
        """验证凭证（Yahoo Finance免费使用）"""
        return True
    
    async def test_connection(self) -> bool:
        """测试连接"""
        try:
            async with aiohttp.ClientSession() as session:
                async with session.get(f"{self.config.base_url}/v8/finance/chart/AAPL", timeout=10) as resp:
                    return resp.status == 200
        except Exception:
            return False
    
    def validate_request(self, params: Dict[str, Any]) -> bool:
        """验证请求参数"""
        required_fields = ['symbol']
        return all(field in params for field in required_fields)
    
    async def fetch_data(self, params: Dict[str, Any]) -> Any:
        """获取原始数据"""
        symbol = params['symbol']
        data_type = params.get('data_type', 'historical')
        
        if data_type == 'historical':
            return await self._fetch_historical_data(symbol, params)
        elif data_type == 'quote':
            return await self._fetch_quote_data(symbol)
        elif data_type == 'info':
            return await self._fetch_company_info(symbol)
        else:
            raise ValueError(f"Unsupported data type: {data_type}")
    
    async def _fetch_historical_data(self, symbol: str, params: Dict[str, Any]) -> Dict[str, Any]:
        """获取历史数据"""
        start_date = params.get('start_date')
        end_date = params.get('end_date')
        interval = params.get('interval', '1d')
        
        # 使用yfinance获取数据
        ticker = yf.Ticker(symbol)
        hist = ticker.history(
            start=start_date,
            end=end_date,
            interval=interval,
            auto_adjust=params.get('adjusted', True),
            prepost=params.get('include_prepost', False)
        )
        
        # 转换为字典格式
        data = {
            'symbol': symbol,
            'data': [],
            'meta': {
                'currency': ticker.info.get('currency', 'USD'),
                'exchange': ticker.info.get('exchange', ''),
                'instrument_type': ticker.info.get('quoteType', 'EQUITY')
            }
        }
        
        for date, row in hist.iterrows():
            data_point = {
                'timestamp': date.isoformat(),
                'open': float(row['Open']) if not pd.isna(row['Open']) else None,
                'high': float(row['High']) if not pd.isna(row['High']) else None,
                'low': float(row['Low']) if not pd.isna(row['Low']) else None,
                'close': float(row['Close']) if not pd.isna(row['Close']) else None,
                'volume': int(row['Volume']) if not pd.isna(row['Volume']) else None,
                'dividends': float(row['Dividends']) if 'Dividends' in row and not pd.isna(row['Dividends']) else None,
                'stock_splits': float(row['Stock Splits']) if 'Stock Splits' in row and not pd.isna(row['Stock Splits']) else None
            }
            data['data'].append(data_point)
        
        return data
    
    async def _fetch_quote_data(self, symbol: str) -> Dict[str, Any]:
        """获取实时报价"""
        ticker = yf.Ticker(symbol)
        info = ticker.info
        
        # 获取最新价格
        hist = ticker.history(period="2d", interval="1d")
        if hist.empty:
            raise ValueError(f"No data available for symbol: {symbol}")
        
        latest = hist.iloc[-1]
        prev_close = hist.iloc[-2]['Close'] if len(hist) > 1 else latest['Close']
        
        return {
            'symbol': symbol,
            'current_price': float(latest['Close']),
            'open': float(latest['Open']),
            'high': float(latest['High']),
            'low': float(latest['Low']),
            'previous_close': float(prev_close),
            'change': float(latest['Close'] - prev_close),
            'change_percent': float((latest['Close'] - prev_close) / prev_close * 100),
            'volume': int(latest['Volume']),
            'market_cap': info.get('marketCap'),
            'currency': info.get('currency', 'USD'),
            'exchange': info.get('exchange', ''),
            'last_trade_time': datetime.now().isoformat()
        }
    
    async def _fetch_company_info(self, symbol: str) -> Dict[str, Any]:
        """获取公司信息"""
        ticker = yf.Ticker(symbol)
        info = ticker.info
        
        return {
            'symbol': symbol,
            'company_name': info.get('longName', ''),
            'business_summary': info.get('longBusinessSummary', ''),
            'industry': info.get('industry', ''),
            'sector': info.get('sector', ''),
            'country': info.get('country', ''),
            'currency': info.get('currency', 'USD'),
            'exchange': info.get('exchange', ''),
            'website': info.get('website', ''),
            'employees': info.get('fullTimeEmployees'),
            'market_cap': info.get('marketCap'),
            'shares_outstanding': info.get('sharesOutstanding'),
            'float_shares': info.get('floatShares'),
            'pe_ratio': info.get('forwardPE') or info.get('trailingPE'),
            'pb_ratio': info.get('priceToBook'),
            'dividend_yield': info.get('dividendYield'),
            'beta': info.get('beta'),
            'ceo': info.get('companyOfficers', [{}])[0].get('name') if info.get('companyOfficers') else None,
            'headquarters': f"{info.get('city', '')}, {info.get('state', '')}, {info.get('country', '')}".strip(', ')
        }
    
    def normalize_data(self, raw_data: Any) -> List[EnhancedPriceData]:
        """标准化数据"""
        if not isinstance(raw_data, dict) or 'data' not in raw_data:
            raise ValueError("Invalid raw data format")
        
        symbol = raw_data['symbol']
        currency_str = raw_data.get('meta', {}).get('currency', 'USD')
        
        # 转换货币代码
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
                dividend_amount=point.get('dividends'),
                split_ratio=point.get('stock_splits', 1.0)
            )
            
            # 计算技术指标（需要足够的历史数据）
            if i >= 20:  # 至少需要20个数据点计算技术指标
                price_data.technical_indicators = self._calculate_technical_indicators(
                    data_points[max(0, i-200):i+1]  # 使用过去200个数据点
                )
            
            # 计算AI特征
            price_data.ai_features = self._calculate_ai_features(
                data_points[max(0, i-20):i+1], i  # 使用过去20个数据点
            )
            
            # 添加AI元数据
            price_data.ai_metadata.add_semantic_tag("provider", "yahoo_finance")
            price_data.ai_metadata.add_semantic_tag("market", "equity")
            price_data.ai_metadata.add_analysis_hint("price_analysis", "suitable for trend and volatility analysis")
            
            normalized_data.append(price_data)
        
        return normalized_data
    
    def _calculate_technical_indicators(self, data_points: List[Dict]) -> TechnicalIndicators:
        """计算技术指标"""
        if len(data_points) < 20:
            return TechnicalIndicators()
        
        # 提取收盘价和成交量
        closes = [p['close'] for p in data_points if p.get('close')]
        volumes = [p['volume'] for p in data_points if p.get('volume')]
        
        if not closes:
            return TechnicalIndicators()
        
        indicators = TechnicalIndicators()
        
        # 简单移动平均线
        if len(closes) >= 5:
            indicators.sma_5 = sum(closes[-5:]) / 5
        if len(closes) >= 10:
            indicators.sma_10 = sum(closes[-10:]) / 10
        if len(closes) >= 20:
            indicators.sma_20 = sum(closes[-20:]) / 20
        if len(closes) >= 50:
            indicators.sma_50 = sum(closes[-50:]) / 50
        if len(closes) >= 200:
            indicators.sma_200 = sum(closes[-200:]) / 200
        
        # 指数移动平均线（简化计算）
        if len(closes) >= 12:
            indicators.ema_12 = self._calculate_ema(closes, 12)
        if len(closes) >= 26:
            indicators.ema_26 = self._calculate_ema(closes, 26)
        
        # MACD
        if indicators.ema_12 and indicators.ema_26:
            indicators.macd = indicators.ema_12 - indicators.ema_26
        
        # RSI（简化计算）
        if len(closes) >= 14:
            indicators.rsi = self._calculate_rsi(closes, 14)
        
        # 布林带
        if len(closes) >= 20:
            sma_20 = sum(closes[-20:]) / 20
            variance = sum((x - sma_20) ** 2 for x in closes[-20:]) / 20
            std_dev = variance ** 0.5
            indicators.bollinger_middle = sma_20
            indicators.bollinger_upper = sma_20 + 2 * std_dev
            indicators.bollinger_lower = sma_20 - 2 * std_dev
        
        # 成交量指标
        if volumes and len(volumes) >= 20:
            indicators.volume_sma = sum(volumes[-20:]) / 20
            if indicators.volume_sma > 0:
                indicators.volume_ratio = volumes[-1] / indicators.volume_sma
        
        return indicators
    
    def _calculate_ema(self, prices: List[float], period: int) -> float:
        """计算指数移动平均线"""
        if len(prices) < period:
            return sum(prices) / len(prices)
        
        multiplier = 2 / (period + 1)
        ema = sum(prices[:period]) / period  # 初始SMA
        
        for price in prices[period:]:
            ema = (price * multiplier) + (ema * (1 - multiplier))
        
        return ema
    
    def _calculate_rsi(self, prices: List[float], period: int = 14) -> float:
        """计算RSI"""
        if len(prices) < period + 1:
            return 50.0  # 中性值
        
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
        volumes = [p['volume'] for p in data_points if p.get('volume')]
        
        if not closes:
            return AIFeatures()
        
        features = AIFeatures()
        
        # 波动率（20日）
        if len(closes) >= 20:
            returns = [(closes[i] - closes[i-1]) / closes[i-1] for i in range(1, len(closes))]
            variance = sum(r ** 2 for r in returns) / len(returns)
            features.volatility = (variance ** 0.5) * (252 ** 0.5)  # 年化波动率
        
        # 动量因子
        if len(closes) >= 2:
            features.momentum_1d = (closes[-1] - closes[-2]) / closes[-2]
        if len(closes) >= 6:
            features.momentum_5d = (closes[-1] - closes[-6]) / closes[-6]
        if len(closes) >= 21:
            features.momentum_20d = (closes[-1] - closes[-21]) / closes[-21]
        
        # 趋势强度
        if len(closes) >= 10:
            # 简单的线性回归趋势
            x_vals = list(range(len(closes)))
            y_vals = closes
            n = len(closes)
            
            sum_x = sum(x_vals)
            sum_y = sum(y_vals)
            sum_xy = sum(x * y for x, y in zip(x_vals, y_vals))
            sum_x2 = sum(x ** 2 for x in x_vals)
            
            slope = (n * sum_xy - sum_x * sum_y) / (n * sum_x2 - sum_x ** 2)
            features.trend_strength = slope / closes[-1]  # 标准化斜率
        
        # 成交量特征
        if volumes and len(volumes) >= 10:
            avg_volume = sum(volumes[-10:]) / 10
            features.volume_profile = volumes[-1] / avg_volume if avg_volume > 0 else 1.0
        
        # 均值回归特征
        if len(closes) >= 20:
            sma_20 = sum(closes[-20:]) / 20
            features.mean_reversion = (closes[-1] - sma_20) / sma_20
        
        # 异常检测分数（基于价格偏离）
        if len(closes) >= 20:
            recent_mean = sum(closes[-20:]) / 20
            recent_std = (sum((x - recent_mean) ** 2 for x in closes[-20:]) / 20) ** 0.5
            if recent_std > 0:
                features.anomaly_score = abs(closes[-1] - recent_mean) / recent_std
        
        return features
    
    def assess_data_quality(self, data: List[EnhancedPriceData]) -> DataQuality:
        """评估数据质量"""
        if not data:
            return DataQuality(
                accuracy_score=0.0,
                completeness_score=0.0,
                timeliness_score=0.0,
                confidence_level=0.5,
                data_sources=["yahoo_finance"]
            )
        
        # 计算完整性
        total_fields = len(data) * 5  # OHLCV 5个字段
        complete_fields = sum(
            sum(1 for field in [dp.open_value, dp.high_value, dp.low_value, dp.close_value, dp.volume] 
                if field is not None) for dp in data
        )
        completeness_score = complete_fields / total_fields if total_fields > 0 else 0.0
        
        # 时效性评估（Yahoo Finance有15分钟延迟）
        if data:
            latest_time = max(dp.timestamp for dp in data)
            time_diff = datetime.now(timezone.utc) - latest_time
            timeliness_score = max(0.0, 1.0 - time_diff.total_seconds() / 3600)  # 1小时内为满分
        else:
            timeliness_score = 0.0
        
        return DataQuality(
            accuracy_score=0.95,  # Yahoo Finance历史准确性较高
            completeness_score=completeness_score,
            timeliness_score=timeliness_score,
            confidence_level=0.9,  # 高可信度
            data_sources=["yahoo_finance"],
            last_updated=datetime.now(timezone.utc)
        )
    
    # 实现抽象方法
    async def get_historical_data(self, symbol: str, start_date: str, end_date: str, **kwargs) -> Any:
        """获取历史数据"""
        params = {
            'symbol': symbol,
            'data_type': 'historical',
            'start_date': start_date,
            'end_date': end_date,
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
                'data_type': 'info',
                **kwargs
            }
            result = await self.get_data(params)
            results.append(result)
        return results
    
    async def screen_stocks(self, criteria: Dict[str, Any], **kwargs) -> Any:
        """股票筛选（Yahoo Finance API限制，简单实现）"""
        symbols = kwargs.get('symbols', ['AAPL', 'GOOGL', 'MSFT', 'TSLA', 'AMZN'])
        results = []
        
        for symbol in symbols:
            try:
                quote = await self.get_real_time_quote([symbol])
                if quote and quote[0]:
                    quote_data = quote[0].data[0] if hasattr(quote[0], 'data') else quote[0]
                    results.append({
                        'symbol': symbol,
                        'current_price': quote_data.get('current_price'),
                        'change_percent': quote_data.get('change_percent'),
                        'volume': quote_data.get('volume'),
                        'market_cap': quote_data.get('market_cap')
                    })
            except Exception as e:
                self.logger.warning(f"Failed to get data for {symbol}: {e}")
                continue
        
        return results
    
    async def get_supported_symbols(self, category: DataCategory) -> List[str]:
        """获取支持的标的列表"""
        # Yahoo Finance支持的热门股票（示例）
        if category == DataCategory.EQUITY:
            return [
                # 美股
                'AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA', 'NFLX',
                # 中概股
                'BABA', 'JD', 'PDD', 'NIO', 'XPEV', 'LI',
                # 港股（需要加后缀）
                '0700.HK', '9988.HK', '3690.HK',
                # A股（需要加后缀）
                '000001.SS', '600036.SS', '600519.SS'
            ]
        elif category == DataCategory.CRYPTO:
            return ['BTC-USD', 'ETH-USD', 'BNB-USD', 'XRP-USD', 'ADA-USD']
        elif category == DataCategory.FOREX:
            return ['EURUSD=X', 'GBPUSD=X', 'USDJPY=X', 'USDCNY=X']
        else:
            return []