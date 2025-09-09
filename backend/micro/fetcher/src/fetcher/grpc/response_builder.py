"""统一的gRPC响应构建器

提供标准化的响应构建方法，确保所有服务返回格式一致的proto对象。
"""

from typing import Any, Dict, List, Optional
from datetime import datetime, timezone
import logging

# 注意：这些导入需要在proto编译完成后生效
# from fetcher.generated import common_pb2, equity_pb2, news_pb2

logger = logging.getLogger(__name__)


class ResponseBuilder:
    """统一的gRPC响应构建器"""
    
    @staticmethod
    def build_success_header(data_count: int = 0, provider_id: str = "") -> Dict[str, Any]:
        """构建成功响应头
        
        Args:
            data_count: 数据条数
            provider_id: 数据提供商ID
            
        Returns:
            响应头字典（临时格式，待proto完成后转换为proto对象）
        """
        return {
            "status": "SUCCESS",
            "response_time": datetime.now(timezone.utc).isoformat(),
            "data_count": data_count,
            "provider": {
                "provider_id": provider_id,
                "provider_name": ResponseBuilder._get_provider_name(provider_id)
            } if provider_id else None,
            "metadata": {}
        }
    
    @staticmethod
    def build_error_header(
        error_message: str, 
        error_code: str = "INTERNAL_ERROR"
    ) -> Dict[str, Any]:
        """构建错误响应头
        
        Args:
            error_message: 错误信息
            error_code: 错误码
            
        Returns:
            错误响应头字典
        """
        return {
            "status": "ERROR",
            "response_time": datetime.now(timezone.utc).isoformat(),
            "data_count": 0,
            "error": {
                "error_code": error_code,
                "error_message": error_message,
                "timestamp": datetime.now(timezone.utc).isoformat()
            },
            "metadata": {}
        }
    
    @staticmethod
    def build_page_info(
        page: int = 1, 
        page_size: int = 50, 
        total_count: int = 0
    ) -> Dict[str, Any]:
        """构建分页信息
        
        Args:
            page: 当前页码
            page_size: 每页大小
            total_count: 总数据量
            
        Returns:
            分页信息字典
        """
        return {
            "page": page,
            "page_size": page_size,
            "total_count": total_count,
            "total_pages": (total_count + page_size - 1) // page_size if page_size > 0 else 0,
            "has_next": page * page_size < total_count,
            "has_previous": page > 1
        }
    
    @staticmethod
    def build_equity_historical_response(
        data_points: List[Any], 
        symbol: str,
        provider_id: str = "yahoo_finance"
    ) -> Dict[str, Any]:
        """构建股票历史数据响应
        
        Args:
            data_points: 价格数据点列表
            symbol: 股票代码
            provider_id: 数据提供商ID
            
        Returns:
            完整的历史数据响应
        """
        try:
            response = {
                "header": ResponseBuilder.build_success_header(
                    data_count=len(data_points),
                    provider_id=provider_id
                ),
                "symbol": symbol,
                "data": [],
                "page_info": ResponseBuilder.build_page_info(
                    page=1,
                    page_size=len(data_points),
                    total_count=len(data_points)
                )
            }
            
            # 转换数据点
            for dp in data_points:
                data_point = ResponseBuilder._convert_price_data(dp)
                if data_point:
                    response["data"].append(data_point)
            
            return response
            
        except Exception as e:
            logger.error(f"构建历史数据响应失败: {e}")
            return {
                "header": ResponseBuilder.build_error_header(
                    f"响应构建失败: {str(e)}"
                ),
                "symbol": symbol,
                "data": [],
                "page_info": ResponseBuilder.build_page_info()
            }
    
    @staticmethod
    def build_equity_quote_response(
        quotes: List[Any],
        symbols: List[str],
        provider_id: str = "yahoo_finance"
    ) -> Dict[str, Any]:
        """构建股票实时行情响应
        
        Args:
            quotes: 行情数据列表
            symbols: 股票代码列表
            provider_id: 数据提供商ID
            
        Returns:
            完整的行情响应
        """
        try:
            response = {
                "header": ResponseBuilder.build_success_header(
                    data_count=len(quotes),
                    provider_id=provider_id
                ),
                "quotes": [],
                "page_info": ResponseBuilder.build_page_info(
                    page=1,
                    page_size=len(quotes),
                    total_count=len(quotes)
                )
            }
            
            # 转换行情数据
            for quote in quotes:
                quote_data = ResponseBuilder._convert_quote_data(quote)
                if quote_data:
                    response["quotes"].append(quote_data)
            
            return response
            
        except Exception as e:
            logger.error(f"构建行情响应失败: {e}")
            return {
                "header": ResponseBuilder.build_error_header(
                    f"响应构建失败: {str(e)}"
                ),
                "quotes": [],
                "page_info": ResponseBuilder.build_page_info()
            }
    
    @staticmethod
    def build_news_response(
        articles: List[Any],
        provider_id: str = "akshare"
    ) -> Dict[str, Any]:
        """构建新闻数据响应
        
        Args:
            articles: 新闻文章列表
            provider_id: 数据提供商ID
            
        Returns:
            完整的新闻响应
        """
        try:
            response = {
                "header": ResponseBuilder.build_success_header(
                    data_count=len(articles),
                    provider_id=provider_id
                ),
                "articles": [],
                "page_info": ResponseBuilder.build_page_info(
                    page=1,
                    page_size=len(articles),
                    total_count=len(articles)
                )
            }
            
            # 转换新闻数据
            for article in articles:
                article_data = ResponseBuilder._convert_news_article(article)
                if article_data:
                    response["articles"].append(article_data)
            
            return response
            
        except Exception as e:
            logger.error(f"构建新闻响应失败: {e}")
            return {
                "header": ResponseBuilder.build_error_header(
                    f"响应构建失败: {str(e)}"
                ),
                "articles": [],
                "page_info": ResponseBuilder.build_page_info()
            }
    
    # 私有辅助方法
    
    @staticmethod
    def _get_provider_name(provider_id: str) -> str:
        """根据提供商ID获取显示名称"""
        provider_names = {
            "yahoo_finance": "Yahoo Finance",
            "akshare": "AKShare",
            "alpha_vantage": "Alpha Vantage",
            "quandl": "Quandl"
        }
        return provider_names.get(provider_id, provider_id.title())
    
    @staticmethod
    def _convert_price_data(data_point: Any) -> Optional[Dict[str, Any]]:
        """转换价格数据点为标准格式"""
        try:
            # 检查是否是EnhancedPriceData对象
            if hasattr(data_point, 'timestamp') and hasattr(data_point, 'close_value'):
                return {
                    "timestamp": data_point.timestamp.isoformat() if hasattr(data_point.timestamp, 'isoformat') else str(data_point.timestamp),
                    "open": float(data_point.open_value) if data_point.open_value is not None else None,
                    "high": float(data_point.high_value) if data_point.high_value is not None else None,
                    "low": float(data_point.low_value) if data_point.low_value is not None else None,
                    "close": float(data_point.close_value) if data_point.close_value is not None else None,
                    "volume": int(data_point.volume) if data_point.volume is not None else None,
                    "adjusted_close": float(getattr(data_point, 'adjusted_close', data_point.close_value)) if hasattr(data_point, 'adjusted_close') or data_point.close_value is not None else None,
                    "change": float(data_point.change) if hasattr(data_point, 'change') and data_point.change is not None else None,
                    "change_percent": float(data_point.change_percent) if hasattr(data_point, 'change_percent') and data_point.change_percent is not None else None,
                    "technical_indicators": ResponseBuilder._extract_technical_indicators(data_point) if hasattr(data_point, 'technical_indicators') else None,
                    "ai_features": ResponseBuilder._extract_ai_features(data_point) if hasattr(data_point, 'ai_features') else None
                }
            
            # 处理字典格式的数据
            elif isinstance(data_point, dict):
                return {
                    "timestamp": data_point.get('timestamp', ''),
                    "open": float(data_point['open']) if data_point.get('open') is not None else None,
                    "high": float(data_point['high']) if data_point.get('high') is not None else None,
                    "low": float(data_point['low']) if data_point.get('low') is not None else None,
                    "close": float(data_point['close']) if data_point.get('close') is not None else None,
                    "volume": int(data_point['volume']) if data_point.get('volume') is not None else None,
                    "adjusted_close": float(data_point.get('adjusted_close', data_point.get('close'))) if data_point.get('adjusted_close') is not None or data_point.get('close') is not None else None,
                    "change": float(data_point['change']) if data_point.get('change') is not None else None,
                    "change_percent": float(data_point['change_percent']) if data_point.get('change_percent') is not None else None
                }
            
            return None
            
        except (ValueError, TypeError, KeyError) as e:
            logger.warning(f"价格数据转换失败: {e}")
            return None
    
    @staticmethod
    def _convert_quote_data(quote: Any) -> Optional[Dict[str, Any]]:
        """转换行情数据为标准格式"""
        try:
            if hasattr(quote, 'symbol') and hasattr(quote, 'close_value'):
                return {
                    "symbol": str(quote.symbol),
                    "current_price": float(quote.close_value) if quote.close_value is not None else None,
                    "open": float(quote.open_value) if hasattr(quote, 'open_value') and quote.open_value is not None else None,
                    "high": float(quote.high_value) if hasattr(quote, 'high_value') and quote.high_value is not None else None,
                    "low": float(quote.low_value) if hasattr(quote, 'low_value') and quote.low_value is not None else None,
                    "volume": int(quote.volume) if hasattr(quote, 'volume') and quote.volume is not None else None,
                    "change": float(quote.change) if hasattr(quote, 'change') and quote.change is not None else None,
                    "change_percent": float(quote.change_percent) if hasattr(quote, 'change_percent') and quote.change_percent is not None else None,
                    "last_trade_time": quote.timestamp.isoformat() if hasattr(quote, 'timestamp') and hasattr(quote.timestamp, 'isoformat') else None,
                    "market_cap": float(getattr(quote, 'market_cap', 0)) if hasattr(quote, 'market_cap') else None,
                    "pe_ratio": float(getattr(quote, 'pe_ratio', 0)) if hasattr(quote, 'pe_ratio') else None
                }
            elif isinstance(quote, dict):
                return {
                    "symbol": str(quote.get('symbol', '')),
                    "current_price": float(quote['current_price']) if quote.get('current_price') is not None else None,
                    "open": float(quote['open']) if quote.get('open') is not None else None,
                    "high": float(quote['high']) if quote.get('high') is not None else None,
                    "low": float(quote['low']) if quote.get('low') is not None else None,
                    "volume": int(quote['volume']) if quote.get('volume') is not None else None,
                    "change": float(quote['change']) if quote.get('change') is not None else None,
                    "change_percent": float(quote['change_percent']) if quote.get('change_percent') is not None else None,
                    "last_trade_time": quote.get('last_trade_time'),
                    "market_cap": float(quote['market_cap']) if quote.get('market_cap') is not None else None,
                    "pe_ratio": float(quote['pe_ratio']) if quote.get('pe_ratio') is not None else None
                }
            
            return None
            
        except (ValueError, TypeError, KeyError) as e:
            logger.warning(f"行情数据转换失败: {e}")
            return None
    
    @staticmethod
    def _convert_news_article(article: Any) -> Optional[Dict[str, Any]]:
        """转换新闻文章为标准格式"""
        try:
            if isinstance(article, dict):
                return {
                    "article_id": f"news_{hash(article.get('title', ''))}",
                    "title": {
                        "zh_cn": article.get('title', ''),
                        "en": article.get('title_en', '')
                    },
                    "summary": {
                        "zh_cn": article.get('summary', article.get('content', '')[:200]),
                        "en": article.get('summary_en', '')
                    },
                    "source": {
                        "source_name": {
                            "zh_cn": article.get('source', ''),
                            "en": article.get('source_en', '')
                        },
                        "website": article.get('url', ''),
                        "credibility": {
                            "credibility_score": article.get('credibility_score', 0.8)
                        }
                    },
                    "published_time": article.get('publish_time', article.get('published_time', '')),
                    "url": article.get('url', ''),
                    "categories": [article.get('category', '财经新闻')],
                    "language": article.get('language', 'zh_cn'),
                    "sentiment": article.get('sentiment', 'neutral'),
                    "keywords": article.get('keywords', []),
                    "related_symbols": article.get('related_symbols', [])
                }
            
            return None
            
        except Exception as e:
            logger.warning(f"新闻文章转换失败: {e}")
            return None
    
    @staticmethod
    def _extract_technical_indicators(data_point: Any) -> Optional[Dict[str, Any]]:
        """提取技术指标"""
        try:
            if hasattr(data_point, 'technical_indicators') and data_point.technical_indicators:
                ti = data_point.technical_indicators
                return {
                    "rsi": float(ti.rsi) if hasattr(ti, 'rsi') and ti.rsi is not None else None,
                    "sma_20": float(ti.sma_20) if hasattr(ti, 'sma_20') and ti.sma_20 is not None else None,
                    "sma_50": float(getattr(ti, 'sma_50', 0)) if hasattr(ti, 'sma_50') else None,
                    "ema_12": float(getattr(ti, 'ema_12', 0)) if hasattr(ti, 'ema_12') else None,
                    "ema_26": float(getattr(ti, 'ema_26', 0)) if hasattr(ti, 'ema_26') else None,
                    "macd": float(ti.macd) if hasattr(ti, 'macd') and ti.macd is not None else None,
                    "macd_signal": float(getattr(ti, 'macd_signal', 0)) if hasattr(ti, 'macd_signal') else None,
                    "macd_histogram": float(getattr(ti, 'macd_histogram', 0)) if hasattr(ti, 'macd_histogram') else None,
                    "bollinger_upper": float(getattr(ti, 'bollinger_upper', 0)) if hasattr(ti, 'bollinger_upper') else None,
                    "bollinger_middle": float(getattr(ti, 'bollinger_middle', 0)) if hasattr(ti, 'bollinger_middle') else None,
                    "bollinger_lower": float(getattr(ti, 'bollinger_lower', 0)) if hasattr(ti, 'bollinger_lower') else None,
                }
            return None
        except Exception as e:
            logger.warning(f"技术指标提取失败: {e}")
            return None
    
    @staticmethod
    def _extract_ai_features(data_point: Any) -> Optional[Dict[str, Any]]:
        """提取AI特征"""
        try:
            if hasattr(data_point, 'ai_features') and data_point.ai_features:
                ai = data_point.ai_features
                return {
                    "volatility": float(ai.volatility) if hasattr(ai, 'volatility') and ai.volatility is not None else None,
                    "trend_strength": float(ai.trend_strength) if hasattr(ai, 'trend_strength') and ai.trend_strength is not None else None,
                    "momentum_1d": float(ai.momentum_1d) if hasattr(ai, 'momentum_1d') and ai.momentum_1d is not None else None,
                    "momentum_5d": float(getattr(ai, 'momentum_5d', 0)) if hasattr(ai, 'momentum_5d') else None,
                    "support_level": float(getattr(ai, 'support_level', 0)) if hasattr(ai, 'support_level') else None,
                    "resistance_level": float(getattr(ai, 'resistance_level', 0)) if hasattr(ai, 'resistance_level') else None,
                    "prediction_confidence": float(getattr(ai, 'prediction_confidence', 0)) if hasattr(ai, 'prediction_confidence') else None
                }
            return None
        except Exception as e:
            logger.warning(f"AI特征提取失败: {e}")
            return None


# 工厂方法和便捷函数

def create_success_response(data: Any, provider_id: str = "") -> Dict[str, Any]:
    """创建成功响应的便捷方法"""
    return {
        "header": ResponseBuilder.build_success_header(
            data_count=len(data) if isinstance(data, (list, tuple)) else 1,
            provider_id=provider_id
        ),
        "data": data
    }


def create_error_response(error_message: str, error_code: str = "INTERNAL_ERROR") -> Dict[str, Any]:
    """创建错误响应的便捷方法"""
    return {
        "header": ResponseBuilder.build_error_header(error_message, error_code),
        "data": None
    }