#!/bin/bash
# Protocol Buffer 编译脚本 
# 编译所有proto文件并生成Python代码

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROTO_DIR="${PROJECT_ROOT}/proto"
CODE_DIR="${PROJECT_ROOT}/src/fetcher"
GENERATED_DIR="${CODE_DIR}/generated"

echo -e "${BLUE}🔨 开始编译 Protocol Buffer 文件...${NC}"

# 检查依赖
check_dependencies() {
    echo -e "${BLUE}🔍 检查依赖...${NC}"
    
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}❌ Python3 未安装${NC}"
        exit 1
    fi
    
    if ! python3 -c "import grpc_tools.protoc" &> /dev/null; then
        echo -e "${YELLOW}⚠️  grpcio-tools 未安装，正在安装...${NC}"
        pip install grpcio-tools
    fi
    
    echo -e "${GREEN}✅ 依赖检查完成${NC}"
}

# 清理旧的生成文件
cleanup_generated() {
    echo -e "${BLUE}🧹 清理旧的生成文件...${NC}"
    
    if [ -d "${GENERATED_DIR}" ]; then
        rm -rf "${GENERATED_DIR}"
    fi
    mkdir -p "${GENERATED_DIR}"
    
    # 创建 __init__.py 文件
    touch "${GENERATED_DIR}/__init__.py"
    
    echo -e "${GREEN}✅ 清理完成${NC}"
}

# 编译proto文件
compile_protos() {
    echo -e "${BLUE}⚙️  编译 Proto 文件...${NC}"
    
    cd "${PROJECT_ROOT}"
    
    # 编译所有proto文件
    proto_files=$(find "${PROTO_DIR}" -name "*.proto")
    
    for proto_file in $proto_files; do
        echo -e "${BLUE}   编译: $(basename $proto_file)${NC}"
        
        python3 -m grpc_tools.protoc \
            --proto_path="${PROTO_DIR}" \
            --python_out="${GENERATED_DIR}" \
            --grpc_python_out="${GENERATED_DIR}" \
            "$proto_file"
    done
    
    echo -e "${GREEN}✅ Proto 文件编译完成${NC}"
}

# 修复导入路径
fix_imports() {
    echo -e "${BLUE}🔧 修复生成文件的导入路径...${NC}"
    
    # 修复相对导入问题
    find "${GENERATED_DIR}" -name "*_pb2_grpc.py" -exec sed -i '' 's/import \([a-zA-Z_]*\)_pb2/from . import \1_pb2/g' {} \;
    
    echo -e "${GREEN}✅ 导入路径修复完成${NC}"
}

# 生成服务存根
generate_stubs() {
    echo -e "${BLUE}📝 生成服务存根...${NC}"
    
    # 为主要服务创建便于使用的包装器
    cat > "${GENERATED_DIR}/client.py" << 'EOF'
"""
gRPC 客户端便捷包装器
"""

import grpc
from typing import AsyncIterator, Dict, Any, Optional
from . import fetcher_service_pb2_grpc
from . import fetcher_service_pb2


class FetcherClient:
    """Fetcher服务客户端"""
    
    def __init__(self, server_address: str = "localhost:50051"):
        self.server_address = server_address
        self.channel: Optional[grpc.aio.Channel] = None
        self.stub: Optional[fetcher_service_pb2_grpc.FetcherServiceStub] = None
    
    async def __aenter__(self):
        self.channel = grpc.aio.insecure_channel(self.server_address)
        self.stub = fetcher_service_pb2_grpc.FetcherServiceStub(self.channel)
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
    ) -> fetcher_service_pb2.FetchResponse:
        """获取外部数据"""
        
        request = fetcher_service_pb2.FetchRequest(
            user_id=user_id,
            workspace_id=workspace_id,
            source_type=source_type,
            source_url=source_url,
            parameters=parameters or {},
            headers=headers or {}
        )
        
        if options:
            request.options.CopyFrom(fetcher_service_pb2.FetchOptions(**options))
        
        return await self.stub.FetchExternalData(request)
    
    async def health_check(self, service: str = "") -> fetcher_service_pb2.HealthCheckResponse:
        """健康检查"""
        request = fetcher_service_pb2.HealthCheckRequest(service=service)
        return await self.stub.HealthCheck(request)


# 便捷函数
async def create_client(server_address: str = "localhost:50051") -> FetcherClient:
    """创建客户端实例"""
    return FetcherClient(server_address)
EOF
    
    echo -e "${GREEN}✅ 服务存根生成完成${NC}"
}

# 生成类型定义
generate_types() {
    echo -e "${BLUE}📋 生成类型定义...${NC}"
    
    cat > "${GENERATED_DIR}/types.py" << 'EOF'
"""
类型定义和常量
"""

from enum import Enum
from typing import Dict, Any, List, Optional
from dataclasses import dataclass
from datetime import datetime


class SourceType(str, Enum):
    """数据源类型"""
    API = "api"
    WEB = "web"
    FILE = "file"
    STREAM = "stream"


class ProcessingType(str, Enum):
    """处理类型"""
    PARSE = "parse"
    TRANSFORM = "transform"
    VALIDATE = "validate"
    ENRICH = "enrich"


class OutputFormat(str, Enum):
    """输出格式"""
    JSON = "json"
    XML = "xml"
    CSV = "csv"
    RAW = "raw"


@dataclass
class FetchConfig:
    """获取配置"""
    timeout_seconds: int = 30
    retry_count: int = 3
    cache_enabled: bool = True
    cache_ttl: str = "3600"
    async_processing: bool = False
    output_format: OutputFormat = OutputFormat.JSON


@dataclass
class BulkConfig:
    """批量处理配置"""
    max_concurrent: int = 10
    stop_on_error: bool = False
    timeout_seconds: int = 300


# 常量定义
DEFAULT_TIMEOUT = 30
DEFAULT_RETRY_COUNT = 3
MAX_CONCURRENT_FETCHES = 50
CACHE_TTL_DEFAULT = "3600"

# 错误代码
ERROR_CODES = {
    "TIMEOUT": "请求超时",
    "NETWORK_ERROR": "网络错误", 
    "INVALID_URL": "无效的URL",
    "AUTHENTICATION_FAILED": "认证失败",
    "RATE_LIMITED": "请求过于频繁",
    "SERVER_ERROR": "服务器错误",
    "DATA_VALIDATION_FAILED": "数据验证失败",
    "UNSUPPORTED_FORMAT": "不支持的格式"
}
EOF
    
    echo -e "${GREEN}✅ 类型定义生成完成${NC}"
}

# 主函数
main() {
    echo -e "${GREEN}🚀 Fetcher Proto 编译器 v1.0${NC}"
    echo -e "${BLUE}项目目录: ${PROJECT_ROOT}${NC}"
    
    check_dependencies
    cleanup_generated
    compile_protos
    fix_imports
    generate_stubs
    generate_types
    
    echo -e "${GREEN}🎉 所有 Proto 文件编译完成！${NC}"
    echo -e "${BLUE}生成的文件位于: ${GENERATED_DIR}${NC}"
    echo -e "${YELLOW}📖 使用方法:${NC}"
    echo -e "${YELLOW}   from fetcher.generated.client import FetcherClient${NC}"
    echo -e "${YELLOW}   async with FetcherClient() as client:${NC}"
    echo -e "${YELLOW}       result = await client.fetch_data(...)${NC}"
}

# 运行主函数
main "$@"