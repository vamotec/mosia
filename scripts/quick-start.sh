#!/bin/bash
# Mosia快速启动脚本 - 独立开发者专用

set -e

echo "⚡ Mosia快速启动"
echo "==============="

# 检查Docker是否运行
if ! docker info &> /dev/null; then
    echo "❌ Docker未运行，请启动Docker Desktop"
    exit 1
fi

# 检查.env文件
if [ ! -f .env ]; then
    echo "⚙️  未找到 .env 文件，正在创建..."
    make env-setup
    echo ""
fi

# 选择启动模式
echo ""
echo "请选择启动模式:"
echo "1) 仅后端开发环境 (API + 数据库)"
echo "2) 完整开发环境 (后端 + 监控)"
echo "3) 开发环境状态检查"
echo "4) 停止所有服务"
echo "5) 重置环境配置"

read -p "请输入选择 (1-5): " choice

case $choice in
    1)
        echo "🚀 启动后端开发环境..."
        docker-compose -f docker-compose.dev.yml up -d api postgres redis kafka zookeeper
        echo ""
        echo "✅ 后端环境已启动!"
        source .env 2>/dev/null || true
        echo "📍 API服务: http://localhost:${API_PORT:-3010}"
        echo "📍 API文档: http://localhost:${API_PORT:-3010}/docs"
        ;;
    2)
        echo "🚀 启动完整开发环境..."
        make dev
        sleep 3
        make monitor
        echo ""
        echo "✅ 完整环境已启动!"
        source .env 2>/dev/null || true
        echo "📍 API服务: http://localhost:${API_PORT:-3010}"
        echo "📊 Grafana监控: http://localhost:3000 (admin/admin)"
        echo "📈 Prometheus: http://localhost:9090"
        ;;
    3)
        echo "📊 检查服务状态..."
        make status
        ;;
    4)
        echo "⏹️  停止所有服务..."
        make dev-stop || true
        make monitor-stop || true
        echo "✅ 所有服务已停止"
        ;;
    5)
        echo "🔄 重置环境配置..."
        make env-setup
        echo "✅ 环境配置已重置，请编辑 .env 文件后重新启动"
        ;;
    *)
        echo "❌ 无效选择，请输入1-5"
        exit 1
        ;;
esac

echo ""
echo "🔧 常用命令:"
echo "  make logs       - 查看服务日志"
echo "  make test       - 运行测试"
echo "  make format     - 格式化代码"
echo "  make status     - 检查服务状态"
echo "  make env-setup  - 重新配置环境变量"
echo "  make help       - 查看所有命令"