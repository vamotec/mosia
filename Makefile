# Mosia项目统一管理 Makefile
# 独立开发者友好的一键操作命令

.PHONY: help dev dev-stop build test clean docker-build docker-clean monitor logs

ifneq (,$(wildcard .env))
 include .env
 export
endif

# Set default values if not provided by .env
DB_DEFAULT_URL ?= jdbc:postgresql://localhost:5432/$(POSTGRES_DB)
DB_DEFAULT_USER ?= $(POSTGRES_USER)
DB_DEFAULT_PASSWORD ?= $(POSTGRES_PASSWORD)

KAFKA_BOOTSTRAP_SERVER ?= $(KAFKA_BOOTSTRAP_SERVER)

SMTP_HOST ?= $(SMTP_HOST)
SMTP_PORT ?= $(SMTP_PORT)
SMTP_USERNAME ?= $(SMTP_USERNAME)
SMTP_PASSWORD ?= $(SMTP_PASSWORD)

SBT := DB_DEFAULT_URL=$(DB_DEFAULT_URL) DB_DEFAULT_USER=$(DB_DEFAULT_USER) DB_DEFAULT_PASSWORD=$(DB_DEFAULT_PASSWORD) sbt

# 默认显示帮助
help:
	@echo "🚀 Mosia项目管理命令"
	@echo ""
	@echo "环境配置:"
	@echo "  make env-setup    设置环境变量配置"
	@echo "  make env-validate 验证环境变量配置"
	@echo ""
	@echo "开发环境:"
	@echo "  make dev          启动开发环境"
	@echo	"  make moscala      启动后端服务"
	@echo "  make dev-stop     停止开发环境"
	@echo "  make logs         查看服务日志"
	@echo "  make status       检查项目状态"
	@echo ""
	@echo "构建和测试:"
	@echo "  make build        构建所有组件"
	@echo "  make test         运行所有测试"
	@echo "  make format       格式化代码"
	@echo ""
	@echo "Docker操作:"
	@echo "  make docker-build 构建Docker镜像"
	@echo "  make docker-clean 清理Docker资源"
	@echo ""
	@echo "监控系统:"
	@echo "  make monitor      启动监控系统"
	@echo "  make monitor-stop 停止监控系统"
	@echo ""
	@echo "维护工具:"
	@echo "  make clean        清理构建产物"
	@echo "  make reset        重置开发环境"
	@echo "  make git-setup    配置Git hooks"

# 开发环境管理
dev:
	@echo "🚀 启动Mosia开发环境..."
	@if [ ! -f .env ]; then \
		echo "⚙️  创建环境配置文件..."; \
		cp .env.example .env; \
		echo "✅ 已创建 .env 文件，可根据需要修改配置"; \
	fi
	docker-compose -f docker-compose.infrastructure.yml up -d
	@echo "✅ 开发环境已启动!"
	@echo "📍 API服务: http://localhost:3010"
	@echo "📍 API文档: http://localhost:3010/docs"
	@echo "📊 查看日志: make logs"

dev-stop:
	@echo "⏹️  停止开发环境..."
	docker-compose -f docker-compose.infrastructure.yml down
	@echo "✅ 开发环境已停止"

logs:
	@echo "📋 查看服务日志 (Ctrl+C退出)..."
	docker-compose -f docker-compose.dev.yml logs -f

moscala:
	@echo "🚀 启动后端服务..."
	cd backend/main_service/moscala && sbt run
	@echo "✅ 后端服务已启动!"
	@echo "📍 API服务: http://localhost:3010"
	@echo "📍 API文档: http://localhost:3010/docs"
	@echo "📊 查看日志: make logs"

# 构建和测试
build:
	@echo "🏗️  构建后端服务..."
	cd backend/main_service/moscala && sbt compile assembly
	@echo "📱 构建移动应用..."
	cd frontend/mobile && flutter build apk
	@echo "✅ 构建完成!"

test:
	@echo "🧪 运行后端测试..."
	cd backend/main_service/moscala && sbt test
	@echo "📱 运行Flutter测试..."
	cd frontend/mobile && flutter test
	@echo "✅ 测试完成!"

format:
	@echo "🎨 格式化Scala代码..."
	cd backend/main_service/moscala && sbt fmt
	@echo "📱 格式化Flutter代码..."
	cd frontend/mobile && dart format .
	@echo "✅ 代码格式化完成!"

# Docker操作
docker-build:
	@echo "🐳 构建Docker镜像..."
	cd backend/main_service/moscala && docker build -t mosia/backend-api .
	@echo "✅ Docker镜像构建完成!"

docker-clean:
	@echo "🧹 清理Docker资源..."
	docker system prune -f
	docker volume prune -f
	@echo "✅ Docker清理完成!"

# 监控系统
monitor:
	@echo "📊 启动监控系统..."
	cd devops/monitoring && docker-compose -f docker-compose.monitoring.yml up -d
	@echo "✅ 监控系统已启动!"
	@echo "📊 Grafana: http://localhost:3000 (admin/admin)"
	@echo "📈 Prometheus: http://localhost:9090"
	@echo "🚨 AlertManager: http://localhost:9093"

monitor-stop:
	@echo "⏹️  停止监控系统..."
	cd devops/monitoring && docker-compose -f docker-compose.monitoring.yml down
	@echo "✅ 监控系统已停止"

# 维护工具
clean:
	@echo "🧹 清理构建产物..."
	cd backend/main_service/moscala && sbt clean
	cd frontend/mobile && flutter clean
	@echo "✅ 清理完成!"

reset: dev-stop docker-clean clean
	@echo "🔄 重置开发环境..."
	@echo "✅ 环境重置完成! 运行 'make dev' 重新启动"

# Git操作
git-setup:
	@echo "📝 配置Git hooks..."
	@echo "#!/bin/sh" > .git/hooks/pre-commit
	@echo "make format" >> .git/hooks/pre-commit
	@chmod +x .git/hooks/pre-commit
	@echo "✅ Git hooks配置完成! 提交前会自动格式化代码"

# 环境变量管理
env-setup:
	@echo "⚙️  设置环境变量配置..."
	@if [ -f .env ]; then \
		echo "⚠️  .env 文件已存在"; \
		read -p "是否覆盖现有配置? [y/N]: " confirm; \
		if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
			cp .env.example .env; \
			echo "✅ 已覆盖 .env 文件"; \
		else \
			echo "保持现有 .env 文件不变"; \
		fi; \
	else \
		cp .env.example .env; \
		echo "✅ 已创建 .env 文件"; \
	fi
	@echo "📝 请编辑 .env 文件配置数据库密码和其他敏感信息"

env-validate:
	@echo "🔍 验证环境变量配置..."
	@if [ ! -f .env ]; then \
		echo "❌ .env 文件不存在，请运行 make env-setup"; \
		exit 1; \
	fi
	@echo "✅ .env 文件存在"
	@echo "📋 当前环境变量:"
	@grep -E "^[^#].*=" .env | head -10 || echo "   (配置为空)"

# 快速状态检查
status:
	@echo "📊 Mosia项目状态:"
	@echo ""
	@echo "📁 环境配置:"
	@if [ -f .env ]; then echo "   ✅ .env 文件已配置"; else echo "   ⚠️  .env 文件不存在，运行 make dev 自动创建"; fi
	@echo ""
	@echo "🐳 Docker容器状态:"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep mosia || echo "   无运行的容器"
	@echo ""
	@echo "💾 磁盘使用:"
	@du -sh . 2>/dev/null || echo "   无法获取大小"
	@echo ""
	@echo "🔧 主要服务端口:"
	@echo "   API服务: http://localhost:$${API_PORT:-3010}"
	@echo "   数据库: localhost:$${POSTGRES_PORT:-5432}"
	@echo "   Redis: localhost:$${REDIS_PORT:-6379}"
	@echo "   Kafka: localhost:$${KAFKA_PORT:-9092}"