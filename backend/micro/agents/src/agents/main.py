"""Main entry point for the Agents microservice."""

import asyncio
import logging
import os
import signal
import sys
from pathlib import Path
from typing import Optional

import uvloop
from dotenv import load_dotenv

from .grpc.server import AgentsGrpcServer
from .config.settings import settings
from .config.logging import setup_logging, get_logger

load_dotenv()

class AgentsMicroService:
    """Fetcher微服务主类"""

    def __init__(self):
        self.server: Optional[AgentsGrpcServer] = None
        self.logger: Optional[logging.Logger] = None
        self._shutdown_event = asyncio.Event()
        self._shutdown_called = False
        self._loop = None

    async def initialize(self):
        """初始化服务"""
        try:
            # 设置日志系统
            setup_logging()
            self.logger = get_logger(__name__)

            self.logger.info("🚀 初始化 Agents 微服务...")

            # 验证环境配置
            await self._validate_environment()

            # 创建并初始化gRPC服务器
            self.server = AgentsGrpcServer(port=settings.agents_grpc_port)
            await self.server.initialize()

            # 设置优雅关闭信号处理
            self._setup_signal_handlers()

            self.logger.info("✅ Agents 微服务初始化完成")

        except Exception as e:
            if self.logger:
                self.logger.error(f"❌ 服务初始化失败: {e}", exc_info=True)
            else:
                print(f"❌ 服务初始化失败: {e}")
            raise

    async def start(self):
        """启动服务"""
        if not self.server:
            raise RuntimeError("服务未初始化，请先调用 initialize()")

        try:
            self.logger.info(f"🎯 启动 Agents gRPC 服务器 (端口: {settings.agents_grpc_port})...")

            # 启动gRPC服务器
            await self.server.start()

            # 等待关闭信号
            await self.wait_for_shutdown()

            # 收到关闭信号后执行清理
            await self.shutdown()

        except Exception as e:
            self.logger.error(f"❌ 服务启动失败: {e}", exc_info=True)
            await self.shutdown()
            raise

    async def shutdown(self):
        """优雅关闭服务"""
        if self._shutdown_called:
            self.logger.info("⚠️ 关闭已在进行中，跳过重复调用")
            return

        self._shutdown_called = True

        if self.logger:
            self.logger.info("🛑 开始关闭 Fetcher 微服务...")

        try:
            if self.server:
                await self.server.stop()

            if self.logger:
                self.logger.info("✅ Fetcher 微服务已成功关闭")

        except Exception as e:
            if self.logger:
                self.logger.error(f"❌ 服务关闭时出现错误: {e}", exc_info=True)
            else:
                print(f"❌ 服务关闭时出现错误: {e}")

    async def wait_for_shutdown(self):
        """等待关闭信号"""
        self.logger.info("🔄 等待关闭信号...")
        await self._shutdown_event.wait()
        self.logger.info("📡 接收到关闭信号，准备执行清理...")

    async def _validate_environment(self):
        """验证环境配置"""
        self.logger.info("🔍 验证环境配置...")

        # 检查必需的环境变量
        required_env_vars = [
            'AGENTS_HOST',
            'AGENTS_GRPC_PORT'
        ]

        missing_vars = []
        for var in required_env_vars:
            if not os.getenv(var):
                missing_vars.append(var)

        if missing_vars:
            self.logger.warning(f"⚠️ 缺少环境变量: {missing_vars}, 将使用默认值")

        # 验证端口可用性
        if not (1024 <= settings.agents_grpc_port <= 65535):
            raise ValueError(f"无效的gRPC端口: {settings.agents_grpc_port}")

        # 检查数据目录权限
        data_dir = Path(settings.data_dir)
        if not data_dir.exists():
            data_dir.mkdir(parents=True, exist_ok=True)
            self.logger.info(f"📁 创建数据目录: {data_dir}")

        if not os.access(data_dir, os.R_OK | os.W_OK):
            raise PermissionError(f"数据目录权限不足: {data_dir}")

        self.logger.info("✅ 环境配置验证通过")

    def _setup_signal_handlers(self):
        """设置信号处理器用于优雅关闭"""

        # 获取当前事件循环
        try:
            self._loop = asyncio.get_running_loop()
        except RuntimeError:
            self._loop = asyncio.get_event_loop()

        def shutdown_callback():
            """关闭回调函数"""
            self.logger.info("📡 异步关闭回调被触发")
            self._shutdown_event.set()

        def handle_signal(signum, frame):
            signal_name = signal.Signals(signum).name
            self.logger.info(f"📡 接收到信号 {signal_name}, 开始优雅关闭...")

            # 线程安全地在事件循环中设置关闭事件
            self._loop.call_soon_threadsafe(shutdown_callback)

        # 注册信号处理器
        for sig in [signal.SIGINT, signal.SIGTERM]:
            signal.signal(sig, handle_signal)

        self.logger.info("📡 信号处理器已设置")

async def main():
    """主函数 - 服务入口点"""
    service = None

    try:
        # 显示启动横幅
        print("\n" + "=" * 60)
        print("🚀 MOSIA Agents MicroService")
        print("   金融智能体微服务")
        print("=" * 60)

        # 创建并启动服务
        service = AgentsMicroService()
        await service.initialize()

        # 启动服务器（这会阻塞直到收到关闭信号）
        await service.start()

    except Exception as e:
        if service and service.logger:
            service.logger.error(f"💥 服务运行出现致命错误: {e}", exc_info=True)
        else:
            print(f"\n💥 服务运行出现致命错误: {e}")
        sys.exit(1)

    finally:
        # 由于start()方法已经处理了shutdown，这里不需要重复调用
        pass


def run_service():
    """同步包装器 - 用于poetry脚本或系统服务"""
    try:
        uvloop.install()
        asyncio.run(main())
    except Exception as e:
        print(f"\n💥 服务异常退出: {e}")
        sys.exit(1)

def disable_telemetry() -> None:
    """
    Disable telemetry / analytics for common Python libraries.
    Call this at the very beginning of your service startup.
    """
    # 通用环境变量（大部分库都会读取这些）
    os.environ["DO_NOT_TRACK"] = "1"
    os.environ["DISABLE_TELEMETRY"] = "1"
    os.environ["NO_TELEMETRY"] = "1"

    # Chroma / ChromaDB
    os.environ["ANONYMIZED_TELEMETRY"] = "False"

    # LangChain
    os.environ["LANGCHAIN_TELEMETRY"] = "false"
    os.environ["LANGCHAIN_ENDPOINT"] = ""
    os.environ["LANGCHAIN_API_KEY"] = ""

    # Hugging Face
    os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"

    # Weights & Biases (wandb)
    os.environ["WANDB_DISABLED"] = "true"

    # OpenTelemetry / OTEL
    os.environ["OTEL_SDK_DISABLED"] = "true"

    # PostHog（部分库会用 posthog 做匿名遥测）
    os.environ["POSTHOG_DISABLE"] = "1"

    # Tensorflow / Pytorch（部分子模块会上传统计）
    os.environ["TF_DISABLE_TELEMETRY"] = "1"
    os.environ["TORCH_DISABLE_TELEMETRY"] = "1"

    # 其他：确保 requests / urllib3 不打印 telemetry 日志
    logging.getLogger("urllib3.connectionpool").setLevel(logging.WARNING)

    print("🚫 Telemetry disabled for common libraries.")

if __name__ == "__main__":
    disable_telemetry()
    run_service()