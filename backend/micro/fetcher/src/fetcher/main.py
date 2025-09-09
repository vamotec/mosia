#!/usr/bin/env python3
"""
Fetcher Microservice - 主入口文件
提供统一的数据获取和处理gRPC服务

支持的数据源：
- Yahoo Finance (股票数据)
- AKShare (中文财经数据)
- Web Scraping (网页抓取)
- API Integration (第三方API集成)

功能特性：
- 异步并发数据获取
- 智能数据处理和增强
- AI驱动的数据分析
- 实时和批量数据处理
- 完整的错误处理和日志记录
"""

import asyncio
import os
import sys
import signal
import logging
from pathlib import Path
from typing import Optional

from dotenv import load_dotenv

from fetcher.config.settings import settings
from fetcher.config.logging import setup_logging, get_logger
from fetcher.grpc.server import FetcherGRPCServer

load_dotenv()

class FetcherMicroService:
    """Fetcher微服务主类"""

    def __init__(self):
        self.server: Optional[FetcherGRPCServer] = None
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

            self.logger.info("🚀 初始化 Fetcher 微服务...")

            # 验证环境配置
            await self._validate_environment()

            # 创建并初始化gRPC服务器
            self.server = FetcherGRPCServer(port=settings.fetcher_grpc_port)
            await self.server.initialize_providers()

            # 设置优雅关闭信号处理
            self._setup_signal_handlers()

            self.logger.info("✅ Fetcher 微服务初始化完成")

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
            self.logger.info(f"🎯 启动 Fetcher gRPC 服务器 (端口: {settings.fetcher_grpc_port})...")

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
            'FETCHER_HOST',
            'FETCHER_GRPC_PORT'
        ]

        missing_vars = []
        for var in required_env_vars:
            if not os.getenv(var):
                missing_vars.append(var)

        if missing_vars:
            self.logger.warning(f"⚠️ 缺少环境变量: {missing_vars}, 将使用默认值")

        # 验证端口可用性
        if not (1024 <= settings.fetcher_grpc_port <= 65535):
            raise ValueError(f"无效的gRPC端口: {settings.fetcher_grpc_port}")

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
        print("🚀 MOSIA Fetcher MicroService")
        print("   数据获取和处理微服务")
        print("=" * 60)

        # 创建并启动服务
        service = FetcherMicroService()
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
        asyncio.run(main())
    except Exception as e:
        print(f"\n💥 服务异常退出: {e}")
        sys.exit(1)


if __name__ == "__main__":
    run_service()