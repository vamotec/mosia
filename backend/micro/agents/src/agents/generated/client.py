"""
gRPC 客户端便捷包装器
"""

import grpc
from typing import AsyncIterator, Dict, Any, Optional
from . import agents_service_pb2_grpc
from . import agents_service_pb2


class AgentsClient:
    """Agents服务客户端"""
    
    def __init__(self, server_address: str = "localhost:50051"):
        self.server_address = server_address
        self.channel: Optional[grpc.aio.Channel] = None
        self.stub: Optional[agents_service_pb2_grpc.AgentsServiceStub] = None
    
    async def __aenter__(self):
        self.channel = grpc.aio.insecure_channel(self.server_address)
        self.stub = agents_service_pb2_grpc.AgentsServiceStub(self.channel)
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.channel:
            await self.channel.close()
    
    async def fetch_data(
        self, 
        user_id: str,
        workspace_id: str, 
        source_type: str,
        source_url: str,
        parameters: Dict[str, str] = None,
        headers: Dict[str, str] = None,
        options: Dict[str, Any] = None
    ) -> agents_service_pb2.FetchResponse:
        """获取外部数据"""
        
        request = agents_service_pb2.FetchRequest(
            user_id=user_id,
            workspace_id=workspace_id,
            source_type=source_type,
            source_url=source_url,
            parameters=parameters or {},
            headers=headers or {}
        )
        
        if options:
            request.options.CopyFrom(agents_service_pb2.FetchOptions(**options))
        
        return await self.stub.FetchExternalData(request)
    
    async def health_check(self, service: str = "") -> agents_service_pb2.HealthCheckResponse:
        """健康检查"""
        request = agents_service_pb2.HealthCheckRequest(service=service)
        return await self.stub.HealthCheck(request)


# 便捷函数
async def create_client(server_address: str = "localhost:50051") -> AgentsClient:
    """创建客户端实例"""
    return AgentsClient(server_address)
