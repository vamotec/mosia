# Mosia项目统一管理 Makefile
# 独立开发者友好的一键操作命令

.PHONY: help dev dev-stop build test clean docker-build docker-clean monitor logs

# 默认显示帮助
help:
	@echo "🚀 Mosia项目管理命令"
	@echo ""
	@echo "开发环境:"
	@echo "  make dev          启动完整开发环境"
	@echo "  make dev-stop     停止开发环境"
	@echo "  make logs         查看服务日志"
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

# 开发环境管理
dev:
	@echo "🚀 启动Mosia开发环境..."
	cd backend/main_service/moscala && docker-compose -f docker-compose.dev.yml up -d
	@echo "✅ 开发环境已启动!"
	@echo "📍 API服务: http://localhost:3010"
	@echo "📍 API文档: http://localhost:3010/docs"
	@echo "📊 查看日志: make logs"

dev-stop:
	@echo "⏹️  停止开发环境..."
	cd backend/main_service/moscala && docker-compose -f docker-compose.dev.yml down
	@echo "✅ 开发环境已停止"

logs:
	@echo "📋 查看服务日志 (Ctrl+C退出)..."
	cd backend/main_service/moscala && docker-compose -f docker-compose.dev.yml logs -f

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

# 快速状态检查
status:
	@echo "📊 Mosia项目状态:"
	@echo ""
	@echo "🐳 Docker容器状态:"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep mosia || echo "   无运行的容器"
	@echo ""
	@echo "💾 磁盘使用:"
	@du -sh . 2>/dev/null || echo "   无法获取大小"
	@echo ""
	@echo "🔧 主要服务端口:"
	@echo "   API服务: http://localhost:3010"
	@echo "   Grafana: http://localhost:3000"
	@echo "   Prometheus: http://localhost:9090"