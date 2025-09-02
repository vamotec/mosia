#!/usr/bin/env bash
set -e

echo "🧹 清理 Scala 项目环境..."

# 删除 Metals / Bloop 相关目录
rm -rf .bloop .metals project/metals.sbt

# 删除 IntelliJ 配置
rm -rf .idea

# 删除 sbt/scala 的编译缓存
find . -name target -type d -exec rm -rf {} +

echo "✅ 已清理 .bloop, .metals, .idea, project/metals.sbt, target"

# 检查是否安装了 sbt
if ! command -v sbt &> /dev/null; then
    echo "⚠️ 未检测到 sbt，请手动运行项目构建。"
    exit 0
fi

echo "🔄 运行 sbt clean compile 重新生成必要的文件..."
sbt clean compile

echo "🎉 清理 & 重建完成，可以重新导入 IntelliJ 项目！"