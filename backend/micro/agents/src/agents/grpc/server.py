"""gRPC server setup for Agents service."""

import asyncio
import signal
from concurrent import futures
from typing import Optional

import grpc
from grpc_health.v1 import health_pb2_grpc
from grpc_health.v1.health_pb2 import HealthCheckResponse

from agents.generated import agents_service_pb2_grpc, financial_agents_pb2_grpc
from .handlers.agents_handler import AgentsServiceHandler
from .handlers.financial_handler import FinancialAgentsHandler
from ..config.settings import settings
from ..config.logging import get_logger, setup_logging
from ..core.agents.content_analyzer import ContentAnalyzer
from ..core.agents.recommendation_engine import RecommendationEngine
from ..core.agents.content_generator import ContentGenerator
from ..core.agents.conversation_ai import ConversationAI


class HealthServicer(health_pb2_grpc.HealthServicer):
    """Health check service implementation."""
    
    def __init__(self, agents_handler: AgentsServiceHandler, financial_handler: FinancialAgentsHandler = None):
        self.agents_handler = agents_handler
        self.financial_handler = financial_handler
        self.logger = get_logger(__name__)
    
    def Check(self, request, context):
        """Handle health check requests."""
        try:
            # Check if all AI engines are ready
            if (self.agents_handler.content_analyzer and 
                self.agents_handler.recommendation_engine and
                self.agents_handler.content_generator and
                self.agents_handler.conversation_ai):
                status = HealthCheckResponse.ServingStatus.SERVING
            else:
                status = HealthCheckResponse.ServingStatus.NOT_SERVING
            
            return HealthCheckResponse(status=status)
            
        except Exception as e:
            self.logger.error("Health check failed", error=str(e))
            return HealthCheckResponse(status=HealthCheckResponse.ServingStatus.NOT_SERVING)


class AgentsGrpcServer:
    """Main gRPC server for Agents service."""
    
    def __init__(self, port: int = 50052):
        self.port = port
        self.logger = get_logger(__name__)
        self.server: Optional[grpc.aio.Server] = None
        self.agents_handler: Optional[AgentsServiceHandler] = None
        self.financial_handler: Optional[FinancialAgentsHandler] = None
        self.health_servicer: Optional[HealthServicer] = None
        self._stopped = asyncio.Event()
        
    async def initialize(self) -> None:
        """Initialize the gRPC server and all AI engines."""
        try:
            # Setup logging
            setup_logging()
            
            # Initialize AI engines
            content_analyzer = ContentAnalyzer()
            await content_analyzer.initialize()
            
            recommendation_engine = RecommendationEngine() 
            await recommendation_engine.initialize()
            
            content_generator = ContentGenerator()
            await content_generator.initialize()
            
            conversation_ai = ConversationAI()
            await conversation_ai.initialize()
            
            # Create handlers
            self.agents_handler = AgentsServiceHandler(
                content_analyzer=content_analyzer,
                recommendation_engine=recommendation_engine,
                content_generator=content_generator,
                conversation_ai=conversation_ai
            )
            
            # Initialize financial handler (with placeholder LLM for now)
            try:
                # TODO: Initialize proper LLM model from config
                # For now, use a placeholder to avoid breaking
                placeholder_llm = None  # This would be the actual LLM model
                financial_config = {
                    "fetcher_host": getattr(settings, 'fetcher_host', 'localhost'),
                    "fetcher_port": getattr(settings, 'fetcher_port', 50051),
                    "online_tools": True
                }
                self.financial_handler = FinancialAgentsHandler(placeholder_llm, financial_config)
                self.logger.info("Financial agents handler initialized")
            except Exception as e:
                self.logger.warning(f"Failed to initialize financial handler: {e}")
                self.financial_handler = None
            
            self.health_servicer = HealthServicer(self.agents_handler, self.financial_handler)
            
            self.logger.info("Agents gRPC server initialized successfully")
            
        except Exception as e:
            self.logger.error("Failed to initialize gRPC server", error=str(e))
            raise
    
    async def start(self) -> None:
        """Start the gRPC server."""
        try:
            # Create gRPC server
            self.server = grpc.aio.server(
                futures.ThreadPoolExecutor(max_workers=settings.performance.grpc_max_workers)
            )
            
            # Add service handlers
            # Note: The actual service registration would happen here with generated protobuf code
            # For now, we'll prepare the structure
            agents_service_pb2_grpc.add_AgentsServiceServicer_to_server(
                self.agents_handler, self.server
            )
            
            # Add financial agents service if available
            if self.financial_handler:
                financial_agents_pb2_grpc.add_FinancialAgentsServiceServicer_to_server(
                    self.financial_handler, self.server
                )
                self.logger.info("Financial agents service registered")
            
            # Add health check service
            health_pb2_grpc.add_HealthServicer_to_server(self.health_servicer, self.server)
            
            # Add server to port
            listen_addr = f"[::]:{self.port}"
            self.server.add_insecure_port(listen_addr)
            
            # Start server
            await self.server.start()
            self.logger.info(f"🚀 Agents gRPC 服务器启动成功: {listen_addr}")
            
        except Exception as e:
            self.logger.error(f"❌ 服务器启动失败: {e}", error=str(e))
            raise
    
    async def stop(self) -> None:
        """Stop the gRPC server gracefully."""
        if self.server:
            self.logger.info("🛑 关闭 gRPC 服务器.")
            await self.server.stop(grace=5)
            self._stopped.set()
            self.logger.info("✅ gRPC 服务器已关闭")
