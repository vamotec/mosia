#!/bin/bash

# Moscala JAR 打包脚本
set -e

echo "🔨 开始打包 Moscala JAR..."

# 进入项目目录
cd "$(dirname "$0")/.."

# 清理之前的构建
echo "🧹 清理之前的构建..."
sbt clean

# 编译项目
echo "⚙️  编译项目..."
sbt compile

# 运行测试
echo "🧪 运行测试..."
sbt test

# 打包 JAR
echo "📦 打包 JAR 文件..."
sbt assembly

# 检查 JAR 文件
if [ -f "target/moscala.jar" ]; then
    echo "✅ JAR 文件已生成: target/moscala.jar"
    echo "📊 文件大小: $(ls -lh target/moscala.jar | awk '{print $5}')"
else
    echo "❌ JAR 文件生成失败"
    exit 1
fi

echo "🎉 打包完成！"