import asyncio
import logging

import grpc

from fetcher.core.providers import provider_manager
from fetcher.core.providers.base import DataCategory
from fetcher.grpc.response_builder import ResponseBuilder


class NewsService:
    """新闻服务实现"""

    def __init__(self):
        self.logger = logging.getLogger("grpc.news_service")

    async def GetNews(self, request, context):
        """获取新闻"""
        try:
            # 解析请求参数
            keywords = list(request.filter.keywords) if request.filter and request.filter.keywords else []
            symbols = list(request.filter.symbols) if request.filter and request.filter.symbols else []
            provider_id = request.provider or "akshare"  # 默认使用AKShare获取中文新闻

            self.logger.info(f"News request: keywords={keywords}, symbols={symbols}")

            # 获取新闻提供商
            providers = provider_manager.get_providers_by_category(DataCategory.NEWS)
            news_provider = None

            for provider in providers:
                if provider.provider_id == provider_id:
                    news_provider = provider
                    break

            if not news_provider:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details(f"News provider {provider_id} not found")
                return None

            # 获取新闻数据
            if symbols:
                response = await news_provider.get_news_by_symbol(symbols=symbols)
            else:
                query = " ".join(keywords) if keywords else ""
                limit = request.page_info.page_size if request.page_info else 50
                response = await news_provider.get_news(query=query, limit=limit)

            # 使用ResponseBuilder构建响应
            articles = response.get('news', []) if isinstance(response, dict) else []
            return ResponseBuilder.build_news_response(
                articles=articles,
                provider_id=provider_id
            )

        except Exception as e:
            self.logger.error(f"Error in GetNews: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))
            return None

    async def StreamNews(self, request, context):
        """流式新闻"""
        try:
            self.logger.info("Starting news stream")

            # 获取最佳新闻提供商
            news_provider = provider_manager.get_best_provider(DataCategory.NEWS)
            if not news_provider:
                raise ValueError("No news providers available")

            # 定期推送新闻
            while True:
                try:
                    response = await news_provider.get_news(query="", limit=10)

                    if hasattr(response, 'data') and response.data:
                        for news_item in response.data[:5]:  # 每次推送最多5条
                            grpc_news = self._build_news_article(news_item)
                            yield grpc_news

                    # 等待一定时间后再次获取
                    await asyncio.sleep(60)  # 1分钟间隔

                except Exception as e:
                    self.logger.error(f"Error in streaming news: {e}")
                    break

        except Exception as e:
            self.logger.error(f"Error in StreamNews: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))