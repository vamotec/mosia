#!/bin/bash
# 加载环境变量脚本

# 检查是否存在根目录的 .env 文件
if [ -f "../../../.env" ]; then
    echo "Loading environment variables from ../../../.env"
    export $(grep -v '^#' ../../../.env | xargs)
elif [ -f ".env" ]; then
    echo "Loading environment variables from ./.env"
    export $(grep -v '^#' .env | xargs)
else
    echo "Warning: No .env file found, using default values"
fi

# 确保必要的数据库环境变量存在
export DB_DEFAULT_URL=${DB_DEFAULT_URL:-"jdbc:postgresql://localhost:5432/mosia_dev"}
export DB_DEFAULT_USER=${DB_DEFAULT_USER:-"mosia"}
export DB_DEFAULT_PASSWORD=${DB_DEFAULT_PASSWORD:-"ttr851217"}

echo "Environment variables set:"
echo "DB_DEFAULT_URL=$DB_DEFAULT_URL"
echo "DB_DEFAULT_USER=$DB_DEFAULT_USER"
echo "DB_DEFAULT_PASSWORD=***"

# 执行传入的命令
exec "$@"