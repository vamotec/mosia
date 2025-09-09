"""
Tushare Provider Implementation
Tushare数据提供商实现 - 中国金融数据专业版
"""

import asyncio
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

import pandas as pd
import tushare as ts

from fetcher.config.logging import get_logger
from fetcher.core.models.base import EnhancedPriceData, TechnicalIndicators, AIFeatures, CurrencyCode
from fetcher.core.providers.base import EquityProvider, NewsProvider, MacroProvider, DataCategory, DataQuality

logger = get_logger(__name__)

class TushareProvider(EquityProvider, NewsProvider, MacroProvider):
    """Tushare数据提供商 - 中国金融数据专业版"""
    def __init__(self, cache_enabled: bool = True, cache_ttl: int = 300,
                 api_version: str = "v8", **kwargs):
        kwargs.setdefault('provider_id', 'tushare')
        kwargs.setdefault('provider_name', 'Tushare')
        kwargs.setdefault('class_path', 'fetcher.core.providers.tushare.provider.TushareProvider')
        kwargs.setdefault('base_url', 'http://api.tushare.pro')
        kwargs.setdefault('supported_categories', ['equity', 'fund', 'bond', 'future', 'option'])
        kwargs.setdefault('supported_regions', ['CN'])
        super().__init__(**kwargs)
        self.cache_enabled = cache_enabled
        self.cache_ttl = cache_ttl
        self.api_version = api_version
        self.pro: Optional[Any] = None

    async def initialize(self):
        """初始化缓存等资源"""
        ts.set_token(self.config.api_key)
        self.pro = ts.pro_api()
        if self.cache_enabled:
            logger.info(f"Tushare 提供商启用缓存，TTL: {self.cache_ttl}秒")

    
    async def validate_credentials(self) -> bool:
        if not self.pro:
            return False
        """验证API凭证"""
        try:
            # 测试获取基础数据
            df = await asyncio.get_event_loop().run_in_executor(None, lambda: self.pro.stock_basic(list_status='L', limit=1)) # type: ignore[misc]
            return not df.empty
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
        
        if data_type in ['historical', 'quote', 'daily']:
            return 'ts_code' in params or 'symbol' in params
        elif data_type == 'company_info':
            return 'ts_code' in params or 'symbol' in params
        elif data_type == 'news':
            return True  # 新闻可以无特定参数
        elif data_type == 'macro':
            return 'indicator' in params or True  # 宏观数据可以有多种参数
        
        return False
    
    async def fetch_data(self, params: Dict[str, Any]) -> Any:
        """获取原始数据"""
        data_type = params.get('data_type', 'historical')
        
        # 在线程池中运行同步的tushare调用
        if data_type == 'historical' or data_type == 'daily':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_historical_data_sync, params
            )
        elif data_type == 'quote':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_quote_data_sync, params
            )
        elif data_type == 'company_info':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_company_info_sync, params
            )
        elif data_type == 'news':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_news_data_sync, params
            )
        elif data_type == 'macro':
            return await asyncio.get_event_loop().run_in_executor(
                None, self._fetch_macro_data_sync, params
            )
        else:
            raise ValueError(f"Unsupported data type: {data_type}")
    
    def _fetch_historical_data_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取历史数据"""
        ts_code = params.get('ts_code') or self._convert_symbol_to_ts_code(params.get('symbol'))
        start_date = params.get('start_date', '20220101')
        end_date = params.get('end_date', datetime.now().strftime('%Y%m%d'))
        
        if not ts_code:
            raise ValueError("ts_code or symbol is required")
        
        try:
            # 获取日线数据
            df = self.pro.daily(
                ts_code=ts_code,
                start_date=start_date,
                end_date=end_date
            )
            
            if df.empty:
                raise ValueError(f"No data found for ts_code: {ts_code}")
            
            # 按日期排序（升序）
            df = df.sort_values('trade_date')
            
            data_list = []
            for _, row in df.iterrows():
                data_point = {
                    'timestamp': pd.to_datetime(str(row['trade_date'])).isoformat(),
                    'open': float(row['open']) if pd.notna(row['open']) else None,
                    'high': float(row['high']) if pd.notna(row['high']) else None,
                    'low': float(row['low']) if pd.notna(row['low']) else None,
                    'close': float(row['close']) if pd.notna(row['close']) else None,
                    'pre_close': float(row['pre_close']) if pd.notna(row['pre_close']) else None,
                    'change': float(row['change']) if pd.notna(row['change']) else None,
                    'pct_chg': float(row['pct_chg']) if pd.notna(row['pct_chg']) else None,
                    'volume': float(row['vol']) * 100 if pd.notna(row['vol']) else None,  # 转换为股数
                    'amount': float(row['amount']) * 1000 if pd.notna(row['amount']) else None  # 转换为元
                }
                data_list.append(data_point)
            
            return {
                'symbol': ts_code,
                'data': data_list,
                'meta': {
                    'currency': 'CNY',
                    'exchange': self._get_exchange_from_ts_code(ts_code),
                    'data_source': 'tushare_pro',
                    'period': 'daily'
                }
            }
            
        except Exception as e:
            self.logger.error(f"Failed to fetch historical data for {ts_code}: {e}")
            raise
    
    def _fetch_quote_data_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取实时行情"""
        ts_codes = params.get('ts_codes')
        if not ts_codes:
            ts_code = params.get('ts_code') or self._convert_symbol_to_ts_code(params.get('symbol'))
            ts_codes = [ts_code] if ts_code else []
        
        if not ts_codes:
            raise ValueError("ts_codes, ts_code or symbol is required")
        
        results = []
        
        try:
            # 获取基础信息
            df_basic = self.pro.stock_basic(list_status='L')
            basic_info = df_basic.set_index('ts_code').to_dict('index')
            
            # 获取最新交易日数据作为实时数据
            latest_date = datetime.now().strftime('%Y%m%d')
            
            for ts_code in ts_codes:
                try:
                    # 获取最近2天的数据
                    df = self.pro.daily(ts_code=ts_code, limit=2)
                    
                    if df.empty:
                        self.logger.warning(f"No quote data found for {ts_code}")
                        continue
                    
                    # 取最新的交易日数据
                    latest = df.iloc[0]
                    prev = df.iloc[1] if len(df) > 1 else latest
                    
                    quote_data = {
                        'ts_code': ts_code,
                        'symbol': ts_code,
                        'name': basic_info.get(ts_code, {}).get('name', ''),
                        'current_price': float(latest['close']),
                        'open': float(latest['open']),
                        'high': float(latest['high']),
                        'low': float(latest['low']),
                        'pre_close': float(latest['pre_close']),
                        'change': float(latest['change']),
                        'pct_chg': float(latest['pct_chg']),
                        'volume': float(latest['vol']) * 100,  # 转换为股数
                        'amount': float(latest['amount']) * 1000,  # 转换为元
                        'trade_date': str(latest['trade_date']),
                        'currency': 'CNY',
                        'exchange': self._get_exchange_from_ts_code(ts_code),
                        'last_trade_time': datetime.now().isoformat()
                    }
                    
                    results.append(quote_data)
                    
                except Exception as e:
                    self.logger.warning(f"Failed to fetch quote for {ts_code}: {e}")
                    continue
            
            return {'quotes': results}
            
        except Exception as e:
            self.logger.error(f"Failed to fetch quote data: {e}")
            raise
    
    def _fetch_company_info_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取公司信息"""
        ts_code = params.get('ts_code') or self._convert_symbol_to_ts_code(params.get('symbol'))
        
        if not ts_code:
            raise ValueError("ts_code or symbol is required")
        
        try:
            # 获取基础信息
            df_basic = self.pro.stock_basic(ts_code=ts_code)
            
            if df_basic.empty:
                raise ValueError(f"No basic info found for {ts_code}")
            
            basic = df_basic.iloc[0]
            
            # 获取公司基本信息
            company_info = {
                'ts_code': ts_code,
                'symbol': ts_code,
                'name': basic.get('name', ''),
                'fullname': basic.get('fullname', ''),
                'enname': basic.get('enname', ''),
                'area': basic.get('area', ''),
                'industry': basic.get('industry', ''),
                'market': basic.get('market', ''),
                'list_date': basic.get('list_date', ''),
                'currency': 'CNY',
                'exchange': self._get_exchange_from_ts_code(ts_code)
            }
            
            # 尝试获取更详细的公司信息
            try:
                df_company = self.pro.stock_company(ts_code=ts_code)
                if not df_company.empty:
                    comp = df_company.iloc[0]
                    company_info.update({
                        'chairman': comp.get('chairman', ''),
                        'manager': comp.get('manager', ''),
                        'secretary': comp.get('secretary', ''),
                        'reg_capital': comp.get('reg_capital', 0),
                        'setup_date': comp.get('setup_date', ''),
                        'province': comp.get('province', ''),
                        'city': comp.get('city', ''),
                        'introduction': comp.get('introduction', ''),
                        'website': comp.get('website', ''),
                        'email': comp.get('email', ''),
                        'office': comp.get('office', ''),
                        'employees': comp.get('employees', 0),
                        'main_business': comp.get('main_business', ''),
                        'business_scope': comp.get('business_scope', '')
                    })
            except Exception as e:
                self.logger.warning(f"Failed to get detailed company info for {ts_code}: {e}")
            
            return company_info
            
        except Exception as e:
            self.logger.error(f"Failed to fetch company info for {ts_code}: {e}")
            raise
    
    def _fetch_news_data_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取新闻数据"""
        try:
            news_list = []
            
            # 获取新闻数据
            try:
                start_date = params.get('start_date', (datetime.now() - pd.Timedelta(days=7)).strftime('%Y%m%d'))
                end_date = params.get('end_date', datetime.now().strftime('%Y%m%d'))
                
                df_news = self.pro.news(
                    start_date=start_date,
                    end_date=end_date,
                    limit=params.get('limit', 100)
                )
                
                for _, row in df_news.iterrows():
                    news_item = {
                        'datetime': row.get('datetime', ''),
                        'content': row.get('content', ''),
                        'title': row.get('title', ''),
                        'channels': row.get('channels', ''),
                        'source': 'tushare_pro',
                        'category': '财经新闻',
                        'language': 'zh_cn'
                    }
                    news_list.append(news_item)
                    
            except Exception as e:
                self.logger.warning(f"Failed to fetch news from tushare: {e}")
            
            return {'news': news_list}
            
        except Exception as e:
            self.logger.error(f"Failed to fetch news data: {e}")
            raise
    
    def _fetch_macro_data_sync(self, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步获取宏观数据"""
        indicator = params.get('indicator', 'gdp')
        start_date = params.get('start_date')
        end_date = params.get('end_date')
        
        try:
            macro_data = []
            
            # 根据指标类型获取不同的宏观数据
            if indicator == 'gdp':
                df = self.pro.cn_gdp()
            elif indicator == 'cpi':
                df = self.pro.cn_cpi()
            elif indicator == 'ppi':
                df = self.pro.cn_ppi()
            elif indicator == 'money_supply':
                df = self.pro.cn_m()
            else:
                # 默认获取GDP数据
                df = self.pro.cn_gdp()
            
            if not df.empty:
                for _, row in df.iterrows():
                    data_point = {
                        'date': row.get('month') or row.get('quarter') or row.get('year'),
                        'value': row.iloc[1] if len(row) > 1 else None,  # 通常第二列是数值
                        'indicator': indicator,
                        'unit': row.get('unit', ''),
                        'source': 'tushare_pro'
                    }
                    macro_data.append(data_point)
            
            return {
                'indicator': indicator,
                'data': macro_data,
                'meta': {
                    'source': 'tushare_pro',
                    'data_type': 'macro_economic'
                }
            }
            
        except Exception as e:
            self.logger.error(f"Failed to fetch macro data for {indicator}: {e}")
            raise
    
    def _convert_symbol_to_ts_code(self, symbol: str) -> Optional[str]:
        """转换股票代码为tushare格式"""
        if not symbol:
            return None
        
        # 如果已经是ts_code格式，直接返回
        if '.' in symbol:
            return symbol
        
        # A股代码转换
        if len(symbol) == 6 and symbol.isdigit():
            if symbol.startswith(('600', '601', '603', '605', '688')):
                return f"{symbol}.SH"  # 上交所
            elif symbol.startswith(('000', '001', '002', '003', '300')):
                return f"{symbol}.SZ"  # 深交所
        
        return symbol
    
    def _get_exchange_from_ts_code(self, ts_code: str) -> str:
        """从ts_code获取交易所"""
        if ts_code.endswith('.SH'):
            return 'SSE'  # 上海证券交易所
        elif ts_code.endswith('.SZ'):
            return 'SZSE'  # 深圳证券交易所
        elif ts_code.endswith('.BJ'):
            return 'BSE'  # 北京证券交易所
        else:
            return 'UNKNOWN'
    
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
                currency=currency,
                change=point.get('change'),
                change_percent=point.get('pct_chg')
            )
            
            # 添加Tushare特有字段
            if point.get('amount'):
                price_data.custom_fields['amount'] = point['amount']
            if point.get('pre_close'):
                price_data.custom_fields['pre_close'] = point['pre_close']
            
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
            price_data.ai_metadata.add_semantic_tag("provider", "tushare_pro")
            price_data.ai_metadata.add_semantic_tag("market", "china")
            price_data.ai_metadata.add_semantic_tag("data_quality", "professional_grade")
            price_data.ai_metadata.add_analysis_hint("data_source", "authoritative_chinese_market_data")
            
            normalized_data.append(price_data)
        
        return normalized_data
    
    def _normalize_quote_data(self, raw_data: Dict) -> List[EnhancedPriceData]:
        """标准化行情数据"""
        normalized_data = []
        
        for quote in raw_data['quotes']:
            currency = CurrencyCode.CNY
            
            price_data = EnhancedPriceData(
                timestamp=datetime.now(),
                symbol=quote['ts_code'],
                provider_id=self.provider_id,
                open_value=quote.get('open'),
                high_value=quote.get('high'),
                low_value=quote.get('low'),
                close_value=quote.get('current_price'),
                volume=quote.get('volume'),
                currency=currency,
                change=quote.get('change'),
                change_percent=quote.get('pct_chg')
            )
            
            # 添加扩展字段
            price_data.custom_fields.update({
                'name': quote.get('name', ''),
                'amount': quote.get('amount'),
                'pre_close': quote.get('pre_close'),
                'trade_date': quote.get('trade_date')
            })
            
            # AI元数据
            price_data.ai_metadata.add_semantic_tag("provider", "tushare_pro")
            price_data.ai_metadata.add_semantic_tag("data_type", "realtime")
            price_data.ai_metadata.add_semantic_tag("market", "china")
            price_data.ai_metadata.add_semantic_tag("quality", "professional")
            
            normalized_data.append(price_data)
        
        return normalized_data
    
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
        if len(closes) >= 50:
            indicators.sma_50 = sum(closes[-50:]) / 50
        
        return indicators
    
    def _calculate_ai_features(self, data_points: List[Dict], current_index: int) -> AIFeatures:
        """计算AI特征"""
        if not data_points:
            return AIFeatures()
        
        closes = [p['close'] for p in data_points if p.get('close')]
        if not closes:
            return AIFeatures()
        
        features = AIFeatures()
        
        # 基本动量计算
        if len(closes) >= 2:
            features.momentum_1d = (closes[-1] - closes[-2]) / closes[-2]
        
        return features
    
    def assess_data_quality(self, data: List[EnhancedPriceData]) -> DataQuality:
        """评估数据质量"""
        if not data:
            return DataQuality(
                accuracy_score=0.0,
                completeness_score=0.0,
                timeliness_score=0.0,
                confidence_level=0.5,
                data_sources=["tushare_pro"]
            )
        
        # Tushare Pro数据质量很高
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
            timeliness_score = max(0.0, 1.0 - time_diff.total_seconds() / 86400)  # 24小时内为满分
        else:
            timeliness_score = 0.0
        
        return DataQuality(
            accuracy_score=0.99,  # Tushare Pro专业级数据，准确性极高
            completeness_score=completeness_score,
            timeliness_score=timeliness_score,
            confidence_level=0.98,  # 非常高的可信度
            data_sources=["tushare_pro"],
            last_updated=datetime.now(timezone.utc)
        )
    
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
        # 转换为ts_code格式
        ts_codes = [self._convert_symbol_to_ts_code(s) for s in symbols]
        params = {
            'ts_codes': ts_codes,
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
        # Tushare Pro提供强大的筛选功能
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
    
    async def get_economic_indicators(self, indicators: List[str], **kwargs) -> Any:
        """获取经济指标"""
        results = []
        for indicator in indicators:
            params = {
                'data_type': 'macro',
                'indicator': indicator,
                **kwargs
            }
            result = await self.get_data(params)
            results.append(result)
        return results
    
    async def get_calendar_events(self, start_date: str, end_date: str, **kwargs) -> Any:
        """获取经济日历事件"""
        # Tushare提供经济事件日历
        params = {
            'data_type': 'macro',
            'indicator': 'calendar',
            'start_date': start_date,
            'end_date': end_date,
            **kwargs
        }
        return await self.get_data(params)
    
    async def get_supported_symbols(self, category: DataCategory) -> List[str]:
        """获取支持的标的列表"""
        if category == DataCategory.EQUITY:
            return [
                # A股主要股票（ts_code格式）
                '000001.SZ', '000002.SZ', '000858.SZ', '600000.SH', '600036.SH', 
                '600519.SH', '600887.SH', '000725.SZ', '002415.SZ', '300014.SZ',
                '002142.SZ', '600276.SH', '000338.SZ', '002304.SZ', '300015.SZ',
                # 科创板
                '688001.SH', '688036.SH', '688111.SH', '688126.SH'
            ]
        else:
            return []