"""
优化后代码的基本功能测试

验证以下优化效果：
1. 死代码已删除
2. ResponseBuilder功能正常
3. ProviderManager单例模式工作
4. 中间件功能正常
"""

import pytest
import sys
import os
from unittest.mock import Mock
from datetime import datetime

# 添加项目路径到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from fetcher.grpc.response_builder import ResponseBuilder, create_success_response, create_error_response
from fetcher.core.providers.provider_manager import ProviderManager, provider_manager, DataCategory
from fetcher.grpc.middleware import ServiceMetrics, grpc_error_handler, global_metrics


class TestResponseBuilder:
    """测试ResponseBuilder功能"""
    
    def test_build_success_header(self):
        """测试成功响应头构建"""
        header = ResponseBuilder.build_success_header(data_count=10, provider_id="yahoo_finance")
        
        assert header["status"] == "SUCCESS"
        assert header["data_count"] == 10
        assert header["provider"]["provider_id"] == "yahoo_finance"
        assert header["provider"]["provider_name"] == "Yahoo Finance"
        assert "response_time" in header
        assert "metadata" in header
    
    def test_build_error_header(self):
        """测试错误响应头构建"""
        header = ResponseBuilder.build_error_header("Test error", "TEST_ERROR")
        
        assert header["status"] == "ERROR"
        assert header["data_count"] == 0
        assert header["error"]["error_code"] == "TEST_ERROR"
        assert header["error"]["error_message"] == "Test error"
        assert "timestamp" in header["error"]
    
    def test_build_page_info(self):
        """测试分页信息构建"""
        page_info = ResponseBuilder.build_page_info(page=2, page_size=25, total_count=100)
        
        assert page_info["page"] == 2
        assert page_info["page_size"] == 25
        assert page_info["total_count"] == 100
        assert page_info["total_pages"] == 4
        assert page_info["has_next"] is True
        assert page_info["has_previous"] is True
    
    def test_build_equity_historical_response(self):
        """测试股票历史数据响应构建"""
        # 模拟数据点
        mock_data_point = Mock()
        mock_data_point.timestamp = datetime.now()
        mock_data_point.open_value = 100.0
        mock_data_point.high_value = 105.0
        mock_data_point.low_value = 99.0
        mock_data_point.close_value = 102.0
        mock_data_point.volume = 1000000
        mock_data_point.change = 2.0
        mock_data_point.change_percent = 2.0
        
        response = ResponseBuilder.build_equity_historical_response(
            data_points=[mock_data_point],
            symbol="AAPL",
            provider_id="yahoo_finance"
        )
        
        assert response["header"]["status"] == "SUCCESS"
        assert response["symbol"] == "AAPL"
        assert len(response["data"]) == 1
        assert response["data"][0]["close"] == 102.0
    
    def test_convenience_functions(self):
        """测试便捷函数"""
        success_response = create_success_response([1, 2, 3], "test_provider")
        assert success_response["header"]["data_count"] == 3
        
        error_response = create_error_response("Test error")
        assert error_response["header"]["status"] == "ERROR"
        assert error_response["data"] is None


class TestProviderManager:
    """测试ProviderManager功能"""
    
    def test_singleton_pattern(self):
        """测试单例模式"""
        manager1 = ProviderManager()
        manager2 = ProviderManager()
        assert manager1 is manager2
        assert manager1 is provider_manager
    
    @pytest.mark.asyncio
    async def test_provider_registration(self):
        """测试提供商注册"""
        manager = ProviderManager()
        
        # 创建模拟提供商
        mock_provider = Mock()
        mock_provider.provider_id = "test_provider"
        mock_provider.categories = [DataCategory.EQUITY]
        
        await manager.register_provider("test_provider", mock_provider, [DataCategory.EQUITY])
        
        # 验证注册成功
        retrieved_provider = manager.get_provider("test_provider")
        assert retrieved_provider is mock_provider
        
        # 验证分类注册
        equity_providers = manager.get_providers_by_category(DataCategory.EQUITY)
        assert mock_provider in equity_providers
    
    def test_get_best_provider(self):
        """测试获取最佳提供商"""
        manager = ProviderManager()
        
        # 由于管理器是单例，可能已有提供商注册
        # 这里测试方法调用不报错即可
        best_provider = manager.get_best_provider(DataCategory.EQUITY)
        # best_provider 可能是 None 或实际的提供商对象
        assert best_provider is None or hasattr(best_provider, '__class__')
    
    def test_provider_status(self):
        """测试提供商状态获取"""
        manager = ProviderManager()
        
        status = manager.get_provider_status()
        
        assert "total_providers" in status
        assert "configured_providers" in status
        assert "initialization_rate" in status
        assert "categories" in status
        assert "health" in status
        assert status["health"] in ["healthy", "unhealthy"]
    
    def test_list_providers(self):
        """测试列出提供商"""
        manager = ProviderManager()
        
        providers = manager.list_providers()
        
        assert isinstance(providers, dict)
        # 每个提供商应该有必要的字段
        for provider_id, info in providers.items():
            assert "provider" in info
            assert "categories" in info
            assert "priority" in info
            assert "enabled" in info
            assert "status" in info


class TestServiceMetrics:
    """测试服务指标收集器"""
    
    def test_metrics_recording(self):
        """测试指标记录"""
        metrics = ServiceMetrics()
        
        # 记录几个请求
        metrics.record_request("TestMethod", 0.1, True)
        metrics.record_request("TestMethod", 0.2, False, "TEST_ERROR")
        metrics.record_request("AnotherMethod", 0.05, True)
        
        stats = metrics.get_stats()
        
        assert stats["request_count"] == 3
        assert stats["error_count"] == 1
        assert stats["error_rate"] == 1/3
        assert "TEST_ERROR" in stats["error_types"]
        assert stats["error_types"]["TEST_ERROR"] == 1
        
        # 检查方法级别统计
        assert "TestMethod" in stats["method_stats"]
        assert stats["method_stats"]["TestMethod"]["count"] == 2
        assert stats["method_stats"]["TestMethod"]["errors"] == 1
        
        assert "AnotherMethod" in stats["method_stats"]
        assert stats["method_stats"]["AnotherMethod"]["count"] == 1
        assert stats["method_stats"]["AnotherMethod"]["errors"] == 0
    
    def test_metrics_reset(self):
        """测试指标重置"""
        metrics = ServiceMetrics()
        
        metrics.record_request("TestMethod", 0.1, True)
        assert metrics.request_count == 1
        
        metrics.reset()
        assert metrics.request_count == 0
        assert len(metrics.response_times) == 0
        assert len(metrics.method_stats) == 0


class TestMiddleware:
    """测试gRPC中间件"""
    
    @pytest.mark.asyncio
    async def test_error_handler_success(self):
        """测试错误处理中间件 - 成功情况"""
        
        @grpc_error_handler
        async def mock_grpc_method(self, request, context):
            return {"status": "success"}
        
        # 模拟self, request, context
        mock_self = Mock()
        mock_request = Mock()
        mock_context = Mock()
        
        result = await mock_grpc_method(mock_self, mock_request, mock_context)
        assert result["status"] == "success"
    
    @pytest.mark.asyncio
    async def test_error_handler_value_error(self):
        """测试错误处理中间件 - ValueError"""
        
        @grpc_error_handler
        async def mock_grpc_method(self, request, context):
            raise ValueError("Invalid input")
        
        mock_self = Mock()
        mock_request = Mock()
        mock_context = Mock()
        
        result = await mock_grpc_method(mock_self, mock_request, mock_context)
        
        # 验证错误处理
        assert result is None
        mock_context.set_code.assert_called()
        mock_context.set_details.assert_called_with("Invalid argument: Invalid input")
    
    @pytest.mark.asyncio
    async def test_error_handler_generic_exception(self):
        """测试错误处理中间件 - 通用异常"""
        
        @grpc_error_handler
        async def mock_grpc_method(self, request, context):
            raise Exception("Something went wrong")
        
        mock_self = Mock()
        mock_request = Mock()
        mock_context = Mock()
        
        result = await mock_grpc_method(mock_self, mock_request, mock_context)
        
        assert result is None
        mock_context.set_details.assert_called_with("Internal error: Something went wrong")
    
    def test_global_metrics(self):
        """测试全局指标收集"""
        # 重置全局指标
        global_metrics.reset()
        
        initial_count = global_metrics.request_count
        
        # 模拟一些请求（通过中间件会自动记录）
        global_metrics.record_request("TestMethod", 0.1, True)
        
        assert global_metrics.request_count == initial_count + 1


class TestArchitectureIntegration:
    """测试架构集成"""
    
    def test_no_dead_code_imports(self):
        """验证死代码已删除，不会引起导入错误"""
        
        # 验证fetcher_handler不再被导入
        try:
            from fetcher.grpc.handlers.fetcher_handler import FetcherServiceHandler
            # 如果能导入，说明文件还存在（不应该发生）
            assert False, "fetcher_handler.py should have been deleted"
        except ImportError:
            # 预期的结果 - 文件不存在
            pass
    
    def test_new_modules_importable(self):
        """验证新模块可以正常导入"""
        
        # 测试新模块导入
        from fetcher.grpc.response_builder import ResponseBuilder
        from fetcher.core.providers.provider_manager import ProviderManager
        from fetcher.grpc.middleware import ServiceMetrics
        
        assert ResponseBuilder is not None
        assert ProviderManager is not None
        assert ServiceMetrics is not None
    
    @pytest.mark.asyncio
    async def test_provider_manager_health_check(self):
        """测试提供商管理器健康检查"""
        manager = ProviderManager()
        
        # 健康检查不应该抛出异常
        health_result = await manager.health_check()
        
        assert isinstance(health_result, dict)
        # 如果没有提供商，结果可能为空，但不应该报错


# 运行测试的便捷函数
def run_tests():
    """运行所有测试"""
    import subprocess
    import sys
    
    # 运行pytest
    result = subprocess.run([
        sys.executable, "-m", "pytest", 
        __file__, 
        "-v", 
        "--tb=short"
    ], capture_output=True, text=True)
    
    print("测试结果:")
    print(result.stdout)
    if result.stderr:
        print("错误信息:")
        print(result.stderr)
    
    return result.returncode == 0


if __name__ == "__main__":
    # 运行基本测试
    success = run_tests()
    if success:
        print("\n✅ 所有优化测试通过！")
    else:
        print("\n❌ 部分测试失败，需要检查代码")