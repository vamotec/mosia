# Mosia Docker Compose 管理
# 简化的容器化开发和部署
ifneq (,$(wildcard .env))
  include .env
  export
endif

.PHONY: help env-setup build sbt test up down restart logs status deploy clean

# 默认帮助
help:
	@echo "🚀 Mosia 容器化管理命令"
	@echo ""
	@echo "环境管理:"
	@echo "  make env-setup    创建 .env 配置文件"
	@echo ""
	@echo "开发环境:"
	@echo "  make up           启动完整容器化环境"
	@echo "  make down         停止容器化环境"
	@echo "  make restart      重启环境"
	@echo "  make logs         查看应用日志"
	@echo "  make logs-all     查看所有服务日志"
	@echo "  make status       查看容器状态"
	@echo ""
	@echo "构建和部署:"
	@echo "  make build        构建 Docker 镜像"
	@echo "  make sbt          运行调试 (本地 SBT)"
	@echo "  make test         运行测试 (本地 SBT)"
	@echo "  make deploy       完整部署+健康检查"
	@echo ""
	@echo "维护:"
	@echo "  make clean        清理 Docker 资源"

# 环境配置
env-setup:
	@echo "⚙️  设置环境配置..."
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "✅ 已创建 .env 文件"; \
	else \
		echo "⚠️  .env 文件已存在"; \
	fi
	@echo "📝 请编辑 .env 文件配置敏感信息"

# Docker Compose 操作
build:
	@echo "🐳 构建 Docker 镜像..."
	@make env-setup
	docker-compose build
	@echo "✅ 构建完成!"

up:
	@echo "🚀 启动容器化环境..."
	@make env-setup
	docker-compose up -d
	@echo "✅ 环境已启动!"
	@echo "📍 API: http://localhost:${API_PORT:-3010}"
	@echo "📍 健康检查: http://localhost:${API_PORT:-3010}/api/health"

down:
	@echo "⏹️  停止容器化环境..."
	docker-compose down
	@echo "✅ 环境已停止"

restart: down up
	@echo "🔄 重启完成!"

logs:
	@echo "📋 查看应用日志..."
	docker-compose logs -f mosia-api

logs-all:
	@echo "📋 查看所有服务日志..."
	docker-compose logs -f

status:
	@echo "📊 容器状态:"
	docker-compose ps

# 本地调试 (使用本地 SBT 更快)
sbt:
	@echo "🧪 运行后端调试..."
	cd backend/main_service/moscala && make run
	@echo "✅ 调试完成!"

# 测试 (使用本地 SBT 更快)
test:
	@echo "🧪 运行后端测试..."
	cd backend/main_service/moscala && make test
	@echo "✅ 测试完成!"

# 完整部署
deploy: build up
	@echo "🚀 执行完整部署..."
	@echo "⏳ 等待服务启动..."
	@sleep 30
	@if curl -f -s http://localhost:${API_PORT:-3010}/api/health; then \
		echo "✅ 部署成功，服务健康!"; \
	else \
		echo "❌ 部署失败，请检查日志..."; \
		exit 1; \
	fi

# 清理
clean:
	@echo "🧹 清理 Docker 资源..."
	docker-compose down -v --remove-orphans
	docker system prune -f
	docker volume prune -f
	@echo "✅ 清理完成!"