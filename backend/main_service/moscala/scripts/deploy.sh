#!/bin/bash

# Moscala 部署脚本
set -e

# 配置参数
REMOTE_HOST="${DEPLOY_HOST:-your-server.com}"
REMOTE_USER="${DEPLOY_USER:-deploy}"
REMOTE_PATH="${DEPLOY_PATH:-/opt/moscala}"
JAR_FILE="target/moscala.jar"
SERVICE_NAME="moscala"

echo "🚀 开始部署 Moscala..."

# 检查 JAR 文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR 文件不存在，请先运行 ./scripts/package-jar.sh"
    exit 1
fi

echo "📤 上传 JAR 文件到服务器..."
scp "$JAR_FILE" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH/moscala.jar"

echo "🔧 部署到服务器..."
ssh "$REMOTE_USER@$REMOTE_HOST" << EOF
    cd $REMOTE_PATH
    
    # 停止服务（如果正在运行）
    if pgrep -f "moscala.jar" > /dev/null; then
        echo "⏹️  停止现有服务..."
        pkill -f "moscala.jar" || true
        sleep 3
    fi
    
    # 备份旧版本
    if [ -f "moscala.jar.old" ]; then
        rm moscala.jar.old
    fi
    if [ -f "moscala.jar.current" ]; then
        mv moscala.jar.current moscala.jar.old
    fi
    
    # 部署新版本
    mv moscala.jar moscala.jar.current
    
    # 启动服务
    echo "▶️  启动新版本..."
    nohup java -jar moscala.jar.current > logs/moscala.log 2>&1 &
    
    # 等待启动
    sleep 5
    
    # 检查服务状态
    if pgrep -f "moscala.jar" > /dev/null; then
        echo "✅ 服务启动成功"
    else
        echo "❌ 服务启动失败，检查日志:"
        tail -n 20 logs/moscala.log
        exit 1
    fi
EOF

echo "🎉 部署完成！"