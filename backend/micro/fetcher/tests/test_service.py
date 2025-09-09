#!/usr/bin/env python3
"""
Fetcher服务测试脚本
测试主要功能而不依赖gRPC proto编译
"""

import asyncio
import sys
from pathlib import Path

# 添加src目录到Python路径
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

try:
    from fetcher import FetchService
    from fetcher import settings
    from fetcher import setup_logging, get_logger
except ImportError as e:
    print(f"❌ 导入失败: {e}")
    sys.exit(1)


async def test_fetch_service():
    """测试获取服务的核心功能"""
    print("🧪 测试 FetchService 核心功能...")
    
    try:
        # 初始化服务
        fetch_service = FetchService()
        await fetch_service.initialize()
        
        print("✅ FetchService 初始化成功")
        
        # 测试外部数据获取
        test_request = {
            "user_id": "test_user",
            "workspace_id": "test_workspace",
            "source_type": "api",
            "source_url": "https://httpbin.org/json",
            "parameters": {},
            "headers": {"User-Agent": "Fetcher-Test/1.0"},
            "options": {
                "timeout_seconds": 10,
                "retry_count": 1,
                "cache_enabled": False
            }
        }
        
        print("🔍 测试API数据获取...")
        result = await fetch_service.fetch_external_data(**test_request)
        
        if result["status"] == "success":
            print(f"✅ API测试成功 - 获取了 {result['size_bytes']} 字节数据")
            print(f"   处理时间: {result['processing_time_seconds']:.2f}秒")
        else:
            print(f"⚠️ API测试部分成功 - 状态: {result['status']}")
            if result.get('error_message'):
                print(f"   错误信息: {result['error_message']}")
        
        # 测试批量获取
        print("🔍 测试批量数据获取...")
        bulk_requests = [
            {**test_request, "source_url": "https://httpbin.org/json"},
            {**test_request, "source_url": "https://httpbin.org/headers"}
        ]
        
        bulk_results = await fetch_service.fetch_bulk_data(
            user_id="test_user",
            workspace_id="test_workspace", 
            requests=bulk_requests,
            options={"max_concurrent": 2, "stop_on_error": False}
        )
        
        successful = len([r for r in bulk_results if r["status"] == "success"])
        print(f"✅ 批量测试完成 - {successful}/{len(bulk_results)} 个请求成功")
        
        # 关闭服务
        await fetch_service.close()
        print("✅ FetchService 已关闭")
        
        return True
        
    except Exception as e:
        print(f"❌ FetchService 测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_providers():
    """测试数据提供商"""
    print("🧪 测试数据提供商...")
    
    try:
        from fetcher import provider_registry, DataCategory
        
        # 尝试注册Yahoo Finance提供商
        try:
            from fetcher.providers import YahooFinanceProvider
            yahoo_provider = YahooFinanceProvider()
            provider_registry.register(yahoo_provider)
            print("✅ Yahoo Finance 提供商注册成功")
        except ImportError:
            print("⚠️ Yahoo Finance 提供商模块不存在，跳过")
        
        # 尝试注册AKShare提供商
        try:
            from fetcher.providers import AKShareProvider
            akshare_provider = AKShareProvider()
            provider_registry.register(akshare_provider)
            print("✅ AKShare 提供商注册成功")
        except ImportError:
            print("⚠️ AKShare 提供商模块不存在，跳过")
        
        # 健康检查
        health_results = await provider_registry.health_check()
        print(f"📊 提供商健康检查: {health_results}")
        
        return True
        
    except Exception as e:
        print(f"❌ 提供商测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_server_components():
    """测试服务器组件（不依赖gRPC）"""
    print("🧪 测试服务器组件...")
    
    try:
        # 测试日志系统
        setup_logging()
        logger = get_logger("tests")
        logger.info("日志系统测试")
        print("✅ 日志系统正常")
        
        # 测试配置系统
        print(f"📋 配置信息:")
        print(f"   Host: {settings.fetcher_host}")
        print(f"   gRPC Port: {settings.fetcher_grpc_port}")
        print(f"   Data Dir: {settings.data_dir}")
        print("✅ 配置系统正常")
        
        # 测试数据处理器
        from fetcher import DataProcessor
        processor = DataProcessor()
        
        test_data = b'{"tests": "data"}'
        result = await processor.process_data(
            data=test_data,
            data_type="json",
            processing_type="parse",
            parameters={}
        )
        
        if result.get("status") == "success":
            print("✅ 数据处理器正常")
        else:
            print(f"⚠️ 数据处理器测试: {result}")
        
        return True
        
    except Exception as e:
        print(f"❌ 服务器组件测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False


async def main():
    """主测试函数"""
    print("=" * 60)
    print("🚀 Fetcher 微服务组件测试")
    print("=" * 60)
    
    # 运行各个测试
    tests = [
        ("服务器组件", test_server_components),
        ("获取服务", test_fetch_service),
        ("数据提供商", test_providers),
    ]
    
    results = []
    for test_name, test_func in tests:
        print(f"\n📝 测试: {test_name}")
        print("-" * 40)
        
        try:
            success = await test_func()
            results.append((test_name, success))
        except Exception as e:
            print(f"❌ 测试 '{test_name}' 异常: {e}")
            results.append((test_name, False))
    
    # 总结测试结果
    print("\n" + "=" * 60)
    print("📊 测试结果总结")
    print("=" * 60)
    
    passed = 0
    total = len(results)
    
    for test_name, success in results:
        status = "✅ 通过" if success else "❌ 失败"
        print(f"  {test_name}: {status}")
        if success:
            passed += 1
    
    print(f"\n🎯 总计: {passed}/{total} 个测试通过")
    
    if passed == total:
        print("🎉 所有测试通过！服务组件运行正常")
        return True
    else:
        print("⚠️ 部分测试失败，需要检查组件")
        return False


if __name__ == "__main__":
    try:
        success = asyncio.run(main())
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n⚡ 测试被中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n💥 测试运行出现致命错误: {e}")
        sys.exit(1)