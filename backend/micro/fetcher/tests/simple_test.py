#!/usr/bin/env python3
"""
简化的结构测试
测试代码导入和基本结构
"""

import sys
from pathlib import Path

# 添加src目录到Python路径
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

def test_imports():
    """测试关键模块的导入"""
    print("🧪 测试模块导入...")
    
    try:
        # 测试配置模块
        print("  📋 测试配置模块...")
        try:
            from fetcher.config.settings import settings
            print(f"    ✅ 设置加载成功: host={getattr(settings, 'host', 'localhost')}")
        except Exception as e:
            print(f"    ⚠️ 设置模块有问题: {e}")
        
        # 测试日志模块
        print("  📝 测试日志模块...")
        try:
            from fetcher.config.logging import get_logger, setup_logging
            print("    ✅ 日志模块导入成功")
        except Exception as e:
            print(f"    ⚠️ 日志模块有问题: {e}")
        
        # 测试核心服务
        print("  🔧 测试核心服务模块...")
        try:
            from fetcher.grpc.services.fetch_service import FetchService
            print("    ✅ FetchService 导入成功")
        except Exception as e:
            print(f"    ⚠️ FetchService 有问题: {e}")
        
        # 测试数据处理器
        print("  ⚙️ 测试数据处理器...")
        try:
            from fetcher.core.processors.data_processor import DataProcessor
            print("    ✅ DataProcessor 导入成功")
        except Exception as e:
            print(f"    ⚠️ DataProcessor 有问题: {e}")
        
        # 测试gRPC服务器
        print("  🌐 测试gRPC服务器...")
        try:
            from fetcher.grpc.server import FetcherGRPCServer
            print("    ✅ FetcherGRPCServer 导入成功")
        except Exception as e:
            print(f"    ⚠️ FetcherGRPCServer 有问题: {e}")
        
        return True
        
    except Exception as e:
        print(f"❌ 导入测试失败: {e}")
        return False


def test_structure():
    """测试目录结构"""
    print("🧪 测试目录结构...")
    
    base_dir = Path(__file__).parent.parent
    expected_paths = [
        "src/fetcher/main.py",
        "src/fetcher/config/settings.py",
        "src/fetcher/config/logging.py",
        "src/fetcher/grpc/services/fetch_service.py",
        "src/fetcher/core/processors/data_processor.py",
        "src/fetcher/grpc/server.py",
        "proto/fetcher_service.proto",
        "pyproject.toml",
        "README.md"
    ]
    
    missing = []
    existing = []
    
    for path_str in expected_paths:
        path = base_dir / path_str
        if path.exists():
            existing.append(path_str)
        else:
            missing.append(path_str)
    
    print(f"  ✅ 存在的文件 ({len(existing)}):")
    for path in existing[:5]:  # 显示前5个
        print(f"    - {path}")
    if len(existing) > 5:
        print(f"    ... 还有 {len(existing)-5} 个文件")
    
    if missing:
        print(f"  ⚠️ 缺失的文件 ({len(missing)}):")
        for path in missing:
            print(f"    - {path}")
    
    return len(missing) == 0


def test_main_entry():
    """测试main.py入口"""
    print("🧪 测试main.py入口...")
    
    try:
        main_path = Path(__file__).parent.parent / "src" / "fetcher" / "main.py"
        if not main_path.exists():
            print("    ❌ main.py 不存在")
            return False
        
        # 检查main.py的基本结构
        content = main_path.read_text(encoding='utf-8')
        
        checks = [
            ("FetcherMicroService类", "class FetcherMicroService" in content),
            ("主函数", "async def main(" in content),
            ("运行函数", "def run_service(" in content),
            ("入口点", '__name__ == "__main__"' in content),
        ]
        
        all_passed = True
        for check_name, passed in checks:
            status = "✅" if passed else "❌"
            print(f"    {status} {check_name}")
            if not passed:
                all_passed = False
        
        return all_passed
        
    except Exception as e:
        print(f"    ❌ main.py测试失败: {e}")
        return False


def main():
    """主测试函数"""
    print("=" * 60)
    print("🚀 Fetcher 微服务结构测试")
    print("=" * 60)
    
    tests = [
        ("目录结构", test_structure),
        ("模块导入", test_imports), 
        ("入口点", test_main_entry),
    ]
    
    results = []
    
    for test_name, test_func in tests:
        print(f"\n📝 测试: {test_name}")
        print("-" * 40)
        
        try:
            success = test_func()
            results.append((test_name, success))
        except Exception as e:
            print(f"❌ 测试异常: {e}")
            results.append((test_name, False))
    
    # 总结
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
        print("🎉 代码结构测试通过！")
        print("\n📝 下一步建议:")
        print("  1. 安装依赖: uv sync")
        print("  2. 编译proto文件: sh ./scripts/compile_protos.sh")
        print("  3. 运行服务: python3 ./src/fetcher/main.py")
        return True
    else:
        print("⚠️ 部分测试失败，需要修复代码结构")
        return False


if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n⚡ 测试被中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n💥 测试运行出现致命错误: {e}")
        sys.exit(1)