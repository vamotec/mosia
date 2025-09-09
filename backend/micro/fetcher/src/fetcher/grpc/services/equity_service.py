import asyncio
from datetime import datetime, timezone
from typing import Dict

import grpc

from fetcher import get_logger
from fetcher.core.providers.provider_manager import provider_manager
from fetcher.grpc.middleware import standard_grpc_method, cached_grpc_method
from fetcher.grpc.response_builder import ResponseBuilder

class EquityService:
    """股票服务实现"""

    def __init__(self):
        self.logger = get_logger("grpc.equity_service")

    @standard_grpc_method
    async def GetHistoricalData(self, request, context):
        """获取历史数据"""
        symbol = request.symbol
        start_date = self._timestamp_to_string(request.start_date)
        end_date = self._timestamp_to_string(request.end_date)
        provider_id = request.provider or "yahoo_finance"

        self.logger.info(f"Historical data request: {symbol} from {provider_id}")

        # 使用新的提供商管理器
        provider = provider_manager.get_provider(provider_id)
        if not provider:
            raise ValueError(f"Provider {provider_id} not found")

        # 获取数据
        response = await provider.get_historical_data(
            symbol=symbol,
            start_date=start_date,
            end_date=end_date,
            adjusted=request.adjusted
        )

        # 使用ResponseBuilder构建响应
        return ResponseBuilder.build_equity_historical_response(
            data_points=response.data if hasattr(response, 'data') else [],
            symbol=symbol,
            provider_id=provider_id
        )

    @cached_grpc_method(ttl_seconds=30)  # 缓存30秒
    async def GetQuote(self, request, context):
        """获取实时行情"""
        symbols = list(request.symbols)
        provider_id = request.provider or "yahoo_finance"

        self.logger.info(f"Quote request: {symbols} from {provider_id}")

        # 使用新的提供商管理器
        provider = provider_manager.get_provider(provider_id)
        if not provider:
            raise ValueError(f"Provider {provider_id} not found")

        # 获取数据
        responses = await provider.get_real_time_quote(symbols=symbols)

        # 使用ResponseBuilder构建响应
        quote_data = []
        if responses:
            for response in responses:
                if hasattr(response, 'data') and response.data:
                    quote_data.extend(response.data)

        return ResponseBuilder.build_equity_quote_response(
            quotes=quote_data,
            symbols=symbols,
            provider_id=provider_id
        )

    async def GetEquityInfo(self, request, context):
        """获取股票信息"""
        try:
            symbols = list(request.symbols)
            provider_id = request.provider or "yahoo_finance"

            self.logger.info(f"Equity info request: {symbols} from {provider_id}")

            # 获取提供商
            provider = provider_manager.get_provider(provider_id)
            if not provider:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details(f"Provider {provider_id} not found")
                return None

            # 获取数据
            responses = await provider.get_company_info(symbols=symbols)

            # 转换为gRPC响应格式
            grpc_response = self._build_info_response(responses, symbols)

            return grpc_response

        except Exception as e:
            self.logger.error(f"Error in GetEquityInfo: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))
            return None

    async def ScreenEquities(self, request, context):
        """股票筛选"""
        try:
            market = request.market
            criteria = self._parse_screener_criteria(request.criteria)
            provider_id = request.provider or "yahoo_finance"

            self.logger.info(f"Equity screening request for market: {market}")

            # 获取提供商
            provider = provider_manager.get_provider(provider_id)
            if not provider:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details(f"Provider {provider_id} not found")
                return None

            # 执行筛选
            results = await provider.screen_stocks(criteria=criteria)

            # 转换为gRPC响应格式
            grpc_response = self._build_screener_response(results, market)

            return grpc_response

        except Exception as e:
            self.logger.error(f"Error in ScreenEquities: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))
            return None

    async def StreamQuotes(self, request, context):
        """流式实时行情"""
        try:
            symbols = list(request.symbols)
            provider_id = request.provider or "yahoo_finance"

            self.logger.info(f"Streaming quotes for: {symbols}")

            # 获取提供商
            provider = provider_manager.get_provider(provider_id)
            if not provider:
                context.set_code(grpc.StatusCode.NOT_FOUND)
                context.set_details(f"Provider {provider_id} not found")
                return

            # 流式推送行情数据
            while True:
                try:
                    responses = await provider.get_real_time_quote(symbols=symbols)

                    for response in responses:
                        if hasattr(response, 'data') and response.data:
                            for quote_data in response.data:
                                grpc_quote = self._build_quote_message(quote_data)
                                yield grpc_quote

                    # 等待一定时间后再次获取
                    await asyncio.sleep(5)  # 5秒间隔

                except Exception as e:
                    self.logger.error(f"Error in streaming quotes: {e}")
                    break

        except Exception as e:
            self.logger.error(f"Error in StreamQuotes: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))

    async def GetBatchHistoricalData(self, request_iterator, context):
        """批量历史数据"""
        try:
            async for request in request_iterator:
                try:
                    # 处理单个请求
                    response = await self.GetHistoricalData(request, context)
                    if response:
                        yield response

                except Exception as e:
                    self.logger.error(f"Error processing batch request: {e}")
                    # 继续处理下一个请求，不中断整个流
                    continue

        except Exception as e:
            self.logger.error(f"Error in GetBatchHistoricalData: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(str(e))

    # 辅助方法
    def _timestamp_to_string(self, timestamp) -> str:
        """转换时间戳为字符串"""
        if not timestamp:
            return ""

        dt = datetime.fromtimestamp(timestamp.seconds + timestamp.nanos / 1e9, tz=timezone.utc)
        return dt.strftime('%Y-%m-%d')

    def _parse_screener_criteria(self, criteria) -> Dict:
        """解析筛选条件"""
        # 这里需要根据实际的proto定义来解析
        # 返回字典格式的筛选条件
        return {}

    def _build_screener_response(self, results, market):
        pass