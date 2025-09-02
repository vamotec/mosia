# Mosia 项目文档索引

欢迎使用 Mosia AI 投资分析平台文档。本文档集合提供了完整的项目概览、技术架构、实施指南和分析报告。

## 📂 文档结构

### 🎯 战略规划 (Strategy)
- [头脑风暴会议纪要](strategy/brainstorming_session_20250824.md) - 项目战略定位与市场探索
- [MVP 开发计划](strategy/mvp_development_plan.md) - 3-4个月MVP开发完整计划

### 🏗️ 系统架构 (Architecture)
- [系统架构设计](architecture/system_architecture.md) - 整体技术架构设计
- [依赖关系分析](architecture/dependency_analysis.md) - 项目依赖关系详细分析
- [协调矩阵](architecture/coordination_matrix.md) - 组件间协调关系

### 🚀 实施指南 (Implementation)
- [16周实施工作流](implementation/16_week_implementation_workflow.md) - 详细的16周开发计划
- [实施指南](implementation/implementation_guide.md) - 具体实施步骤和最佳实践
- [质量门控验证框架](implementation/quality_gates_validation_framework.md) - 代码质量控制体系

### ⚙️ 运维部署 (Operations)
- [部署指南](operations/DEPLOYMENT.md) - JAR部署配置和环境设置

### 📊 分析报告 (Reports)
- [后端技术分析报告](reports/Mosia_Backend_Analysis_First_20250831.md) - Scala后端深度技术分析
- [DevOps容器分析报告](reports/DevOps_Container_Analysis_20250831.md) - 容器化部署技术分析

## 🚀 快速导航

### 新用户入门
1. 阅读 [MVP开发计划](strategy/mvp_development_plan.md) 了解项目概览
2. 查看 [系统架构设计](architecture/system_architecture.md) 理解技术栈
3. 参考 [部署指南](operations/DEPLOYMENT.md) 设置开发环境

### 开发人员
1. 查看 [16周实施工作流](implementation/16_week_implementation_workflow.md) 了解开发流程
2. 阅读 [后端技术分析](reports/Mosia_Backend_Analysis_First_20250831.md) 理解后端架构
3. 参考 [质量门控框架](implementation/quality_gates_validation_framework.md) 确保代码质量

### 运维人员
1. 阅读 [DevOps分析报告](reports/DevOps_Container_Analysis_20250831.md) 了解基础设施
2. 查看 [部署指南](operations/DEPLOYMENT.md) 掌握部署流程

## 📈 项目概览

**Mosia** 是一个基于AI的金融投资分析平台，采用现代化技术栈：

- **后端**: Scala 3.3.6 + ZIO生态系统
- **前端**: Flutter跨平台移动应用
- **数据库**: PostgreSQL + Redis缓存
- **消息队列**: Apache Kafka
- **容器化**: Docker + Kubernetes
- **CI/CD**: GitHub Actions
- **监控**: Prometheus + Grafana

## 🎯 核心特性

- 智能AI对话投资分析
- 多券商投资组合数据集成  
- 个性化投资建议生成
- 实时市场数据分析
- 安全的用户认证与授权
- 跨平台移动端体验

---

**最后更新**: 2025年9月2日  
**文档版本**: v1.0  
**项目阶段**: 早期开发阶段