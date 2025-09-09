#!/usr/bin/env python3
"""
Fetcher项目安装和配置脚本
"""

import os
import sys
import subprocess
import argparse
from pathlib import Path


def run_command(cmd, check=True):
    """运行系统命令"""
    print(f"执行命令: {cmd}")
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if check and result.returncode != 0:
        print(f"命令执行失败: {result.stderr}")
        sys.exit(1)
    return result


def setup_python_environment():
    """设置Python环境"""
    print("正在设置Python虚拟环境...")
    
    # 检查Python版本
    python_version = sys.version_info
    if python_version < (3, 9):
        print("错误: 需要Python 3.9或更高版本")
        sys.exit(1)
    
    print(f"Python版本检查通过: {python_version.major}.{python_version.minor}")
    
    # 创建虚拟环境（如果不存在）
    venv_path = Path("venv")
    if not venv_path.exists():
        run_command("python -m venv venv")
        print("虚拟环境创建成功")
    
    # 激活虚拟环境的指令
    if os.name == 'nt':  # Windows
        activate_cmd = "venv\\Scripts\\activate"
    else:  # Unix/Linux/macOS
        activate_cmd = "source venv/bin/activate"
    
    print(f"请手动激活虚拟环境: {activate_cmd}")
    
    # 升级pip
    run_command("python -m pip install --upgrade pip")
    
    # 安装依赖
    if Path("requirements.txt").exists():
        run_command("pip install -r requirements.txt")
        print("Python依赖安装完成")
    else:
        print("警告: 未找到requirements.txt文件")


def compile_proto_files():
    """编译Protocol Buffers文件"""
    print("正在编译Protocol Buffers文件...")
    
    proto_dir = Path("proto")
    output_dir = Path("src/generated")
    
    if not proto_dir.exists():
        print("错误: proto目录不存在")
        return
    
    # 创建输出目录
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # 编译所有proto文件
    proto_files = list(proto_dir.glob("*.proto"))
    if not proto_files:
        print("警告: 未找到proto文件")
        return
    
    for proto_file in proto_files:
        cmd = f"""python -m grpc_tools.protoc \
            --proto_path=proto \
            --python_out=src/generated \
            --grpc_python_out=src/generated \
            {proto_file}"""
        run_command(cmd)
    
    # 创建__init__.py文件
    init_file = output_dir / "__init__.py"
    init_file.touch()
    
    print("Protocol Buffers文件编译完成")


def setup_database():
    """设置数据库"""
    print("正在设置数据库...")
    
    # 这里可以添加数据库初始化逻辑
    # 例如运行migrations、创建表等
    
    print("数据库设置完成（如需要，请手动配置数据库连接）")


def setup_redis():
    """检查Redis连接"""
    print("正在检查Redis连接...")
    
    try:
        import redis
        r = redis.Redis(host='localhost', port=6379, db=0)
        r.ping()
        print("Redis连接正常")
    except Exception as e:
        print(f"Redis连接失败: {e}")
        print("请确保Redis服务正在运行")


def setup_config():
    """设置配置文件"""
    print("正在设置配置文件...")
    
    config_dir = Path("config")
    config_dir.mkdir(exist_ok=True)
    
    # 创建示例配置文件
    sample_config = config_dir / "config.sample.yaml"
    if not sample_config.exists():
        config_content = """# Fetcher配置文件示例
server:
  host: "0.0.0.0"
  port: 50051
  max_workers: 10

providers:
  yahoo_finance:
    enabled: true
    rate_limit: 100
    timeout: 30
  
  akshare:
    enabled: true
    rate_limit: 60
    timeout: 30

redis:
  host: "localhost"
  port: 6379
  db: 0

database:
  url: "postgresql://postgres:password@localhost:5432/fetcher"

logging:
  level: "INFO"
  format: "%(asctime)s - %(name)s - %(levelname)s - %(message)s"

ai:
  enable_analysis: true
  analysis_cache_ttl: 300  # 5分钟缓存
"""
        sample_config.write_text(config_content, encoding='utf-8')
        print("示例配置文件已创建: config/config.sample.yaml")
    
    # 创建环境变量文件
    env_file = Path(".env.sample")
    if not env_file.exists():
        env_content = """# 环境变量示例文件
ENVIRONMENT=development
LOG_LEVEL=DEBUG
REDIS_URL=redis://localhost:6379/0
DATABASE_URL=postgresql://postgres:password@localhost:5432/fetcher

# API Keys (可选)
YAHOO_FINANCE_API_KEY=
ALPHA_VANTAGE_API_KEY=
POLYGON_API_KEY=

# 安全配置
SECRET_KEY=your-secret-key-here
"""
        env_file.write_text(env_content, encoding='utf-8')
        print("示例环境变量文件已创建: .env.sample")


def setup_docker():
    """设置Docker环境"""
    print("正在设置Docker环境...")
    
    # 检查Docker是否安装
    result = run_command("docker --version", check=False)
    if result.returncode != 0:
        print("警告: Docker未安装或未启动")
        return
    
    # 检查Docker Compose是否安装
    result = run_command("docker-compose --version", check=False)
    if result.returncode != 0:
        print("警告: Docker Compose未安装")
        return
    
    print("Docker环境检查通过")
    
    # 可以选择构建Docker镜像
    build_docker = input("是否构建Docker镜像? (y/n): ").lower() == 'y'
    if build_docker:
        run_command("docker-compose build")
        print("Docker镜像构建完成")


def run_tests():
    """运行测试"""
    print("正在运行测试...")
    
    if Path("tests").exists():
        run_command("python -m pytest tests/ -v")
        print("测试运行完成")
    else:
        print("未找到测试目录")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="Fetcher项目设置脚本")
    parser.add_argument("--skip-python", action="store_true", help="跳过Python环境设置")
    parser.add_argument("--skip-proto", action="store_true", help="跳过Proto编译")
    parser.add_argument("--skip-docker", action="store_true", help="跳过Docker设置")
    parser.add_argument("--tests", action="store_true", help="运行测试")
    
    args = parser.parse_args()
    
    print("=" * 50)
    print("Fetcher金融数据获取服务 - 项目设置")
    print("=" * 50)
    
    try:
        if not args.skip_python:
            setup_python_environment()
            print()
        
        if not args.skip_proto:
            compile_proto_files()
            print()
        
        setup_config()
        print()
        
        setup_database()
        print()
        
        setup_redis()
        print()
        
        if not args.skip_docker:
            setup_docker()
            print()
        
        if args.test:
            run_tests()
            print()
        
        print("=" * 50)
        print("设置完成!")
        print("=" * 50)
        print()
        print("接下来的步骤:")
        print("1. 复制config/config.sample.yaml为config/config.yaml并修改配置")
        print("2. 复制.env.sample为.env并设置环境变量")
        print("3. 启动Redis服务器")
        print("4. 运行服务器: python -m fetcher.services.grpc_server")
        print("5. 或使用Docker: docker-compose up")
        
    except KeyboardInterrupt:
        print("\n设置过程被用户中断")
        sys.exit(1)
    except Exception as e:
        print(f"\n设置过程中出现错误: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()