"""
Fetcher gRPC Server Implementation
整合的数据获取gRPC服务器实现
"""
import asyncio
from concurrent import futures
from typing import Dict, Optional

import grpc
from grpc_health.v1 import health_pb2_grpc
from grpc_health.v1.health_pb2 import HealthCheckResponse
from grpc_reflection.v1alpha import reflection

from ..config.logging import get_logger, log_grpc_call
from ..core.processors.data_processor import DataProcessor
from ..core.providers.provider_manager import provider_manager
from ..generated import fetcher_service_pb2_grpc
from .services.equity_service import EquityService
from .services.fetch_service import FetchService
from .services.news_service import NewsService

if not hasattr(grpc, "__version__"):
    import pkg_resources
    grpc.__version__ = pkg_resources.get_distribution("grpcio").version

class HealthServicer(health_pb2_grpc.HealthServicer):
    """健康检查服务实现"""
    
    def __init__(self, services_health: Dict[str, bool]):
        self.services_health = services_health
        self.logger = get_logger(__name__)
    
    def Check(self, request, context):
        """处理健康检查请求"""
        service = request.service if hasattr(request, 'service') else ""
        
        if service == "" or service in self.services_health:
            # 检查所有服务或特定服务的健康状态
            if service == "":
                all_healthy = all(self.services_health.values())
                status = HealthCheckResponse.ServingStatus.SERVING if all_healthy else HealthCheckResponse.ServingStatus.NOT_SERVING
            else:
                status = HealthCheckResponse.ServingStatus.SERVING if self.services_health.get(service, False) else HealthCheckResponse.ServingStatus.NOT_SERVING
            
            return HealthCheckResponse(status=status)
        else:
            return HealthCheckResponse(status=HealthCheckResponse.ServingStatus.SERVICE_UNKNOWN)

class FetcherServiceServicer:
    """统一的Fetcher服务实现 - 整合所有数据获取功能"""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.fetch_service = FetchService()
        self.data_processor = DataProcessor()
        self.equity_service = EquityService()
        self.news_service = NewsService()
    
    async def initialize(self):
        """初始化服务组件"""
        await self.fetch_service.initialize()
        self.logger.info("FetcherService初始化完成")
    
    async def close(self):
        """关闭服务组件"""
        await self.fetch_service.close()
    
    # 统一数据获取方法
    async def FetchExternalData(self, request, context):
        """处理外部数据获取请求"""
        log_grpc_call("FetchExternalData", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id,
            "source_type": request.source_type
        })
        
        try:
            result = await self.fetch_service.fetch_external_data(
                user_id=request.user_id,
                workspace_id=request.workspace_id,
                source_type=request.source_type,
                source_url=request.source_url,
                parameters=dict(request.parameters),
                headers=dict(request.headers),
                options=dict(request.options) if hasattr(request, 'options') else {}
            )
            return result
            
        except Exception as e:
            self.logger.error(f"外部数据获取失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"数据获取失败: {str(e)}")
            return None

    async def FetchBulkData(self, request, context):
        """处理批量数据获取请求"""
        log_grpc_call("FetchBulkData", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id,
            "requests_count": len(request.requests)
        })
        
        try:
            # 批量处理多个请求
            for fetch_request in request.requests:
                result = await self.fetch_service.fetch_external_data(
                    user_id=fetch_request.user_id,
                    workspace_id=fetch_request.workspace_id,
                    source_type=fetch_request.source_type,
                    source_url=fetch_request.source_url,
                    parameters=dict(fetch_request.parameters),
                    headers=dict(fetch_request.headers),
                    options=dict(fetch_request.options) if hasattr(fetch_request, 'options') else {}
                )
                yield result
                
        except Exception as e:
            self.logger.error(f"批量数据获取失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"批量数据获取失败: {str(e)}")
            return

    async def ScheduleFetch(self, request, context):
        """处理定时获取请求"""
        log_grpc_call("ScheduleFetch", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id,
            "schedule": request.schedule
        })
        
        try:
            # TODO: 实现定时任务调度
            from ..generated.fetcher_service_pb2 import ScheduleFetchResponse
            from google.protobuf.timestamp_pb2 import Timestamp
            import time
            
            # 暂时返回成功响应
            response = ScheduleFetchResponse()
            response.schedule_id = f"schedule_{request.user_id}_{hash(request.schedule_name)}"
            response.success = True
            response.message = "定时任务已创建"
            
            # 设置下次运行时间（示例：1小时后）
            next_run = Timestamp()
            next_run.FromSeconds(int(time.time() + 3600))
            response.next_run.CopyFrom(next_run)
            
            return response
            
        except Exception as e:
            self.logger.error(f"定时任务创建失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"定时任务创建失败: {str(e)}")
            return None

    async def ProcessData(self, request, context):
        """处理数据处理请求"""
        log_grpc_call("ProcessData", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id,
            "data_type": request.data_type,
            "processing_type": request.processing_type
        })
        
        try:
            result = await self.data_processor.process_data(
                data=request.raw_data,
                data_type=request.data_type,
                processing_type=request.processing_type,
                parameters=dict(request.parameters)
            )
            return result
            
        except Exception as e:
            self.logger.error(f"数据处理失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"数据处理失败: {str(e)}")
            return None

    async def ValidateData(self, request, context):
        """处理数据验证请求"""
        log_grpc_call("ValidateData", {
            "user_id": request.user_id,
            "schema_type": request.schema_type
        })
        
        try:
            from ..generated.fetcher_service_pb2 import ValidationResponse
            from google.protobuf.timestamp_pb2 import Timestamp
            import time
            
            # TODO: 实现数据验证逻辑
            response = ValidationResponse()
            response.is_valid = True
            
            # 设置验证时间
            validated_at = Timestamp()
            validated_at.FromSeconds(int(time.time()))
            response.validated_at.CopyFrom(validated_at)
            
            return response
            
        except Exception as e:
            self.logger.error(f"数据验证失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"数据验证失败: {str(e)}")
            return None

    async def EnrichData(self, request, context):
        """处理数据增强请求"""
        log_grpc_call("EnrichData", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id,
            "data_type": request.data_type,
            "enrichment_types": list(request.enrichment_types)
        })
        
        try:
            from ..generated.fetcher_service_pb2 import EnrichmentResponse
            from google.protobuf.timestamp_pb2 import Timestamp
            import time
            
            # TODO: 实现数据增强逻辑
            response = EnrichmentResponse()
            response.enrichment_id = f"enrich_{request.user_id}_{hash(str(request.data))}"
            response.enriched_data = request.data  # 暂时返回原数据
            
            # 设置增强时间
            enriched_at = Timestamp()
            enriched_at.FromSeconds(int(time.time()))
            response.enriched_at.CopyFrom(enriched_at)
            
            return response
            
        except Exception as e:
            self.logger.error(f"数据增强失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"数据增强失败: {str(e)}")
            return None

    async def StreamData(self, request, context):
        """处理数据流请求"""
        log_grpc_call("StreamData", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id,
            "stream_type": request.stream_type
        })
        
        try:
            from ..generated.fetcher_service_pb2 import StreamResponse
            from google.protobuf.timestamp_pb2 import Timestamp
            import asyncio
            import time
            
            # 模拟数据流
            for i in range(5):  # 发送5条测试数据
                response = StreamResponse()
                response.stream_id = f"stream_{request.user_id}_{i}"
                response.status = "data"
                response.data = f"stream data {i}".encode()
                response.data_type = "text"
                
                # 设置时间戳
                timestamp = Timestamp()
                timestamp.FromSeconds(int(time.time()))
                response.timestamp.CopyFrom(timestamp)
                
                yield response
                await asyncio.sleep(1)  # 模拟实时数据间隔
                
        except Exception as e:
            self.logger.error(f"数据流处理失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"数据流处理失败: {str(e)}")
            return

    async def ManageWebhook(self, request, context):
        """处理Webhook管理请求"""
        log_grpc_call("ManageWebhook", {
            "user_id": request.user_id,
            "workspace_id": request.workspace_id,
            "action": request.action,
            "webhook_url": request.webhook_url
        })
        
        try:
            from ..generated.fetcher_service_pb2 import WebhookResponse
            from google.protobuf.timestamp_pb2 import Timestamp
            import time
            
            response = WebhookResponse()
            response.webhook_id = f"webhook_{request.user_id}_{hash(request.webhook_url)}"
            response.success = True
            response.message = f"Webhook {request.action} 操作成功"
            response.status = "active"
            
            # 设置创建时间
            created_at = Timestamp()
            created_at.FromSeconds(int(time.time()))
            response.created_at.CopyFrom(created_at)
            
            return response
            
        except Exception as e:
            self.logger.error(f"Webhook管理失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Webhook管理失败: {str(e)}")
            return None

    async def HealthCheck(self, request, context):
        """处理健康检查请求"""
        log_grpc_call("HealthCheck", {"service": request.service})
        
        try:
            from ..generated.fetcher_service_pb2 import HealthCheckResponse
            from google.protobuf.timestamp_pb2 import Timestamp
            import time
            
            response = HealthCheckResponse()
            response.status = "SERVING"
            response.details["service"] = request.service or "fetcher"
            response.details["status"] = "healthy"
            
            # 设置时间戳
            timestamp = Timestamp()
            timestamp.FromSeconds(int(time.time()))
            response.timestamp.CopyFrom(timestamp)
            
            return response
            
        except Exception as e:
            self.logger.error(f"健康检查失败: {e}", exc_info=True)
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"健康检查失败: {str(e)}")
            return None
    
    # 委托给专门的服务处理器
    async def GetHistoricalData(self, request, context):
        """获取历史股票数据"""
        return await self.equity_service.GetHistoricalData(request, context)
    
    async def GetQuote(self, request, context):
        """获取实时行情"""
        return await self.equity_service.GetQuote(request, context)
    
    async def GetNews(self, request, context):
        """获取新闻数据"""
        return await self.news_service.GetNews(request, context)

class FetcherGRPCServer:
    """整合的Fetcher gRPC服务器"""
    
    def __init__(self, port: int = 50051):
        self.port = port
        self._server: Optional[grpc.aio.Server] = None
        self._stopped = asyncio.Event()
        self.logger = get_logger(__name__)
        self.services_health = {"fetcher": False, "equity": False, "news": False}
        
        # 初始化主服务
        self.fetcher_service = FetcherServiceServicer()
        self.health_service = HealthServicer(self.services_health)
    
    async def initialize_providers(self):
        """初始化数据提供商"""
        try:
            self.logger.info("🔧 初始化数据提供商.")
            # 使用新的提供商管理器初始化所有提供商
            await provider_manager.initialize_all()
            
            # 初始化主服务
            await self.fetcher_service.initialize()

            # 更新健康状态
            provider_status = provider_manager.get_provider_status()
            if provider_status['health'] == 'healthy':
                self.services_health.update({"fetcher": True, "equity": True, "news": True})
            else:
                self.logger.warning("⚠️ 部分提供商初始化失败")
                self.services_health.update({"fetcher": False, "equity": False, "news": False})
            
            self.logger.info("✅ 数据提供商初始化完成")
            
        except Exception as e:
            self.logger.error(f"❌ 提供商初始化失败: {e}", exc_info=True)
            raise
    
    async def start(self):
        """启动gRPC服务器"""
        try:
            # 创建gRPC服务器
            self._server = grpc.aio.server(
                futures.ThreadPoolExecutor(max_workers=10)
            )
            
            # 注册统一接口服务
            fetcher_service_pb2_grpc.add_FetcherServiceServicer_to_server(
                self.fetcher_service, self._server
            )
            self.logger.info("✅ FetcherService 注册成功")
            
            # 注册健康检查服务
            health_pb2_grpc.add_HealthServicer_to_server(self.health_service, self._server)
            
            # 添加反射服务
            service_names = (
                'fetcher.FetcherService',
                'grpc.health.v1.Health',
                reflection.SERVICE_NAME,
            )
            reflection.enable_server_reflection(service_names, self._server)
            
            # 监听端口
            listen_addr = f'[::]:{self.port}'
            self._server.add_insecure_port(listen_addr)
            
            # 启动服务器
            await self._server.start()
            self.logger.info(f"🚀 Fetcher gRPC 服务器启动成功: {listen_addr}")
            
            # 不再自己等待终止，由上层服务控制
                
        except Exception as e:
            self.logger.error(f"❌ 服务器启动失败: {e}", exc_info=True)
            raise
    
    async def stop(self):
        """停止服务器"""
        if self._server:
            self.logger.info("🛑 关闭 gRPC 服务器.")
            
            # 关闭服务
            await self.fetcher_service.close()
            
            # 更新健康状态
            for service in self.services_health:
                self.services_health[service] = False
            
            await self._server.stop(grace=5)
            self._stopped.set()
            self.logger.info("✅ gRPC 服务器已关闭")