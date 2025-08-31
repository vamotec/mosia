# 🚀 Mosia - AI-Enhanced Collaborative Workspace

> **智能协作工作空间平台** - 结合AI能力的下一代团队协作解决方案

[![Backend CI](https://github.com/username/mosia/workflows/Backend%20CI/badge.svg)](https://github.com/username/mosia/actions)
[![Frontend CI](https://github.com/username/mosia/workflows/Frontend%20CI/badge.svg)](https://github.com/username/mosia/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📋 项目概述

Mosia是一个现代化的AI增强协作工作空间，为团队提供智能化的项目管理和协作工具。

### 🎯 核心功能
- **智能协作**: AI驱动的团队协作工具
- **项目管理**: 敏捷项目管理和任务跟踪
- **实时通信**: 集成聊天和视频会议
- **文档管理**: 智能文档编辑和版本控制
- **数据分析**: 团队效率和项目洞察

### 🏗️ 技术架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Flutter App   │    │   Scala API     │    │  Python AI      │
│   (Frontend)    │◄──►│   (Backend)     │◄──►│  (Microservice) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                    ┌─────────┼─────────┐
                    │         │         │
              ┌──────▼──┐ ┌───▼───┐ ┌───▼────┐
              │PostgreSQL│ │ Redis │ │ Kafka  │
              └─────────┘ └───────┘ └────────┘
```

## 🚀 快速开始

### 📋 环境要求

- **Java**: 21+
- **Scala**: 3.3.6
- **Flutter**: 3.24.0+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+

### ⚡ 一键启动开发环境

```bash
# 克隆项目
git clone https://github.com/你的用户名/mosia.git
cd mosia

# 启动完整开发环境
make dev

# 或者手动启动
cd backend/main_service/moscala
docker-compose -f docker-compose.dev.yml up -d
```

### 🌐 访问服务

| 服务 | 地址 | 描述 |
|------|------|------|
| **API服务** | http://localhost:3010 | 后端API接口 |
| **API文档** | http://localhost:3010/docs | Swagger接口文档 |
| **数据库** | localhost:5432 | PostgreSQL |
| **缓存** | localhost:6379 | Redis |
| **消息队列** | localhost:9092 | Kafka |

### 📊 监控系统

```bash
# 启动监控系统
make monitor

# 访问监控面板
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
# AlertManager: http://localhost:9093
```

---

## 📁 项目结构

```
mosia/
├── 📱 frontend/mobile/          # Flutter移动应用
├── 🖥️  backend/
│   ├── main_service/moscala/    # Scala主服务 (ZIO+Tapir)
│   └── micro_service/           # Python AI微服务
├── 🚀 devops/                  # DevOps和基础设施
│   ├── k8s/                    # Kubernetes配置
│   ├── monitoring/             # Prometheus+Grafana
│   └── terraform/              # AWS基础设施
├── 📖 docs/                    # 项目文档和计划
├── 🎨 assets/                  # UI设计和品牌资源
├── 🔧 .github/workflows/       # CI/CD配置
├── 📝 Makefile                 # 统一项目管理
└── 📄 README.md               # 项目说明
```

---

## 💻 开发工作流

### 🔄 日常开发

```bash
# 1. 启动开发环境
make dev

# 2. 开发代码
# 编辑 backend/main_service/moscala/src/main/scala/...
# 编辑 frontend/mobile/lib/...

# 3. 格式化代码
make format

# 4. 运行测试
make test

# 5. 提交代码
git add .
git commit -m "feat: 添加新功能"
git push origin main
```

### 🚀 CI/CD自动化

**代码推送后自动执行**:
1. ✅ **代码编译** - Scala编译检查
2. ✅ **格式检查** - 代码风格验证
3. ✅ **测试运行** - 单元测试和集成测试
4. ✅ **安全扫描** - 依赖漏洞检查
5. ✅ **构建镜像** - Docker镜像构建
6. ✅ **推送仓库** - GitHub Container Registry

---

## 🛠️ 技术栈

### 🖥️ 后端技术

| 组件 | 技术 | 版本 |
|------|------|------|
| **语言** | Scala | 3.3.6 |
| **框架** | ZIO + ZIO-HTTP | 2.x |
| **API** | Tapir + Swagger | Latest |
| **数据库** | PostgreSQL + Quill | 15+ |
| **缓存** | Redis | 7+ |
| **消息队列** | Apache Kafka | Latest |
| **认证** | JWT + OAuth2 | - |

### 📱 前端技术

| 组件 | 技术 | 版本 |
|------|------|------|
| **框架** | Flutter | 3.24.0+ |
| **语言** | Dart | 3.0+ |
| **状态管理** | Riverpod | Latest |
| **网络** | Dio + Retrofit | Latest |

### 🚀 DevOps技术

| 组件 | 技术 | 版本 |
|------|------|------|
| **容器化** | Docker + Compose | 20.10+ |
| **编排** | Kubernetes | 1.28+ |
| **CI/CD** | GitHub Actions | - |
| **监控** | Prometheus + Grafana | Latest |
| **基础设施** | Terraform + AWS | Latest |

---

## 🔧 开发环境配置

### 🏠 本地开发

**最小配置**:
```bash
# 只需要Docker和Git
git clone <repository>
make dev
```

**完整配置** (可选):
```bash
# 安装开发工具
brew install scala sbt flutter terraform kubectl
```

### ☁️ 云端开发

**GitHub Codespaces** (推荐):
- 一键启动完整开发环境
- 预配置所有开发工具
- 无需本地安装任何软件

**本地VSCode + Dev Container**:
- 使用`.devcontainer`配置
- 统一的开发环境设置

---

## 📊 项目状态

### ✅ 已完成功能

**第1周 (后端基础)** ✅:
- [x] Scala API服务框架
- [x] 用户认证系统 (JWT + OAuth)
- [x] 数据库架构和迁移
- [x] Docker化开发环境
- [x] CI/CD管道设置
- [x] 基础监控系统

### 🔄 进行中功能

**第2周 (前端基础)**:
- [ ] Flutter应用框架
- [ ] 用户界面设计实现
- [ ] API集成和状态管理
- [ ] 用户认证流程

### 📅 计划功能

参见 [`docs/16_week_implementation_workflow.md`](docs/16_week_implementation_workflow.md) 获取完整开发计划。

---

## 🤝 贡献指南

### 🔧 开发环境设置

1. **Fork并克隆仓库**
2. **启动开发环境**: `make dev`
3. **创建功能分支**: `git checkout -b feature/功能名`
4. **开发和测试**: `make test`
5. **提交PR**: 详细描述变更内容

### 📝 代码规范

- **Scala**: 遵循官方代码风格，使用scalafmt
- **Flutter**: 遵循Dart官方风格指南
- **提交信息**: 使用常规提交格式 (conventional commits)
- **测试**: 新功能必须包含相应测试

---

## 📞 支持和联系

- **问题报告**: [GitHub Issues](https://github.com/你的用户名/mosia/issues)
- **功能请求**: [GitHub Discussions](https://github.com/你的用户名/mosia/discussions)
- **开发文档**: [`docs/`](docs/) 目录
- **API文档**: http://localhost:3010/docs (开发环境)

---

## 📄 许可证

本项目采用 [MIT许可证](LICENSE) - 详见LICENSE文件。

---

## 🙏 致谢

感谢所有为Mosia项目做出贡献的开发者和设计师。

---

**⭐ 如果这个项目对你有帮助，请给个Star!**