#!/bin/bash
# Mosia独立开发者环境设置脚本

set -e

echo "🚀 Mosia开发环境设置"
echo "====================="

# 检查必需工具
check_command() {
    if ! command -v $1 &> /dev/null; then
        echo "❌ $1 未安装，请先安装: $2"
        exit 1
    else
        echo "✅ $1 已安装"
    fi
}

echo "🔍 检查开发工具..."
check_command "docker" "https://docs.docker.com/get-docker/"
check_command "docker-compose" "Docker Desktop附带"
check_command "git" "系统自带或 brew install git"

# 可选工具检查
echo ""
echo "🔍 检查可选开发工具..."
if command -v sbt &> /dev/null; then
    echo "✅ SBT已安装 - 可以本地开发Scala"
else
    echo "⚠️  SBT未安装 - 建议安装以提高开发效率: brew install sbt"
fi

if command -v flutter &> /dev/null; then
    echo "✅ Flutter已安装 - 可以开发移动应用"
else
    echo "⚠️  Flutter未安装 - 移动应用开发需要: https://flutter.dev/docs/get-started/install"
fi

# 创建必需的目录
echo ""
echo "📁 创建必需目录..."
mkdir -p logs
mkdir -p data/postgres
mkdir -p data/redis
mkdir -p data/kafka

# 复制环境配置
echo ""
echo "⚙️  设置环境配置..."
if [ ! -f .env ]; then
    cp .env.example .env
    echo "✅ 已创建 .env 文件，请根据需要修改配置"
else
    echo "✅ .env 文件已存在"
fi

# Git hooks设置
echo ""
echo "🔧 设置Git hooks..."
if [ -d .git ]; then
    make git-setup
    echo "✅ Git hooks已配置 - 提交前自动格式化代码"
else
    echo "⚠️  未检测到Git仓库，跳过Git hooks设置"
fi

echo ""
echo "🎉 环境设置完成!"
echo ""
echo "📋 下一步操作:"
echo "1. 编辑 .env 文件，配置数据库密码等"
echo "2. 运行 'make dev' 启动开发环境"
echo "3. 访问 http://localhost:3010 查看API"
echo "4. 访问 http://localhost:3000 查看监控(可选)"
echo ""
echo "📖 更多信息请查看 README.md"