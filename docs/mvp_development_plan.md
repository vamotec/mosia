# Mosia MVP开发计划

**文档版本**: v1.0  
**创建时间**: 2025-08-24  
**文档类型**: 产品开发规划  
**预计开发周期**: 3-4个月

## 🎯 MVP战略概述

### 核心验证目标
- **价值假设验证**: 华人投资者对个性化AI投资助手的真实需求
- **产品差异化验证**: 相比富途等现有工具的独特价值
- **技术可行性验证**: 在合规前提下AI能提供的投资洞察价值
- **商业模式验证**: 用户获取和留存的可行路径

### MVP策略定位
**智能优先的混合方案**
- **核心焦点**: AI对话和个性化分析能力
- **数据策略**: 手动输入为主，API集成为辅
- **用户体验**: 朋友式AI助手的完整体验
- **技术路径**: 平衡功能价值与开发复杂度

---

## 📊 功能优先级架构

### P0级别 - 核心价值功能 (必须实现)

#### 🤖 AI投资对话系统
**开发时间**: 4-6周  
**开发优先级**: 最高  
**技术栈**: Python + OpenAI/Claude API + 投资知识库

**功能需求**:
- **朋友式对话界面**: 支持自然语言的投资讨论
- **上下文记忆**: 记住对话历史，保持话题连贯性
- **投资知识集成**: 预置华人投资者关心的投资概念和市场知识
- **情感识别**: 识别用户情绪状态，调整回应方式
- **合规边界**: 严格遵循"信息服务"模式，避免具体投资建议

**关键特性**:
```
用户: "我想买苹果股票"
AI: "苹果确实是个好公司！💪 不过我注意到你现在科技股已经占了60%，
     再加苹果的话风险可能有点集中。要不要考虑先买点其他行业的？
     或者告诉我为什么特别看好苹果？我们一起分析分析 🤔"
```

#### 📱 基础用户系统
**开发时间**: 2-3周  
**开发优先级**: 高  
**技术栈**: Scala + ZIO + PostgreSQL + Flutter

**功能需求**:
- **用户注册登录**: 邮箱/手机号注册，JWT认证
- **个人资料管理**: 投资经验、风险偏好、投资目标设置
- **偏好设置**: 通知频率、交互风格、语言偏好
- **安全机制**: 密码加密、会话管理、隐私保护

#### 📊 投资组合输入系统  
**开发时间**: 2-3周  
**开发优先级**: 高  
**技术栈**: Flutter + Scala + PostgreSQL

**功能需求**:
- **手动持仓输入**: 股票代码、持仓数量、成本价格输入
- **组合展示**: 持仓列表、当前价值、盈亏状况
- **批量导入**: 支持CSV/Excel文件批量导入
- **数据验证**: 股票代码验证、价格合理性检查
- **编辑功能**: 持仓的增加、删除、修改操作

#### 🎯 个性化投资分析  
**开发时间**: 3-4周  
**开发优先级**: 最高  
**技术栈**: Python + 金融数据API + AI分析引擎

**功能需求**:
- **组合风险分析**: 集中度分析、行业分布、波动性评估
- **个性化洞察**: 基于用户风险偏好的组合评价
- **市场关联分析**: 持仓股票与市场整体的相关性分析
- **简化呈现**: 复杂分析结果的可视化和通俗化表达
- **对话集成**: 分析结果与AI对话系统的无缝集成

**P0总开发时间: 11-16周 (约3-4个月)**

---

### P1级别 - 重要功能 (用户体验提升)

#### 🔔 智能主动提醒
**开发时间**: 3-4周  
**价值**: 展示AI的proactive特性，提升用户粘性

**触发场景**:
- 持仓股票重大新闻事件
- 市场异常波动影响用户组合
- 投资组合风险异常增加
- 定期的投资健康检查

#### 📈 实时市场数据集成  
**开发时间**: 2-3周  
**数据源**: Alpha Vantage, Yahoo Finance, IEX Cloud

**功能**:
- 实时股价数据获取
- 基本面财务数据
- 市场新闻和公告信息

#### 🧠 用户偏好学习系统
**开发时间**: 3-4周  
**技术**: 用户行为分析 + 机器学习

**功能**:
- 对话模式偏好学习
- 投资关注点识别  
- 个性化内容推荐

---

### P2级别 - 竞争优势 (后续版本)

#### 🔗 券商API集成
- **优先级**: Interactive Brokers API
- **功能**: 自动获取真实持仓数据
- **合规**: 只读权限，严格数据保护

#### 📊 高级分析功能  
- 相关性分析、回测分析
- 行业轮动分析
- 风险价值(VaR)计算

#### 👥 华人投资社区
- 匿名讨论功能
- 投资心得分享
- 热门话题追踪

---

## 🗓️ P0开发时间线 (详细)

### 第1-2周: 基础架构搭建
```
技术栈设置:
- Scala后端框架 (ZIO + Tapir + Quill)
- Flutter移动端初始化
- PostgreSQL数据库设计
- Python微服务架构

开发任务:
- 项目结构初始化
- 数据库Schema设计
- 基础API框架搭建
- CI/CD流程设置
```

### 第3-4周: 用户系统开发
```
后端开发:
- 用户注册/登录API
- JWT认证系统
- 用户资料管理
- 权限控制系统

前端开发:
- 注册/登录界面
- 用户资料设置页面
- 主导航框架
- 基础UI组件库
```

### 第5-6周: 投资组合系统
```
后端开发:
- 投资组合数据模型
- 持仓CRUD API
- 数据验证逻辑
- 批量导入功能

前端开发:
- 持仓输入界面
- 组合展示页面
- 数据编辑功能
- CSV导入界面
```

### 第7-10周: AI对话系统核心开发
```
Python微服务:
- LLM接口封装 (OpenAI/Claude)
- 投资知识库构建
- 对话管理系统
- 上下文记忆机制

前端集成:
- 聊天界面开发
- 消息渲染组件
- 实时通信(WebSocket)
- 用户体验优化
```

### 第11-12周: 个性化分析引擎
```
Python分析服务:
- 组合风险分析算法
- 个性化评价逻辑
- 市场数据集成
- 分析结果生成

前端展示:
- 分析结果可视化
- 图表组件开发
- 交互式数据展示
- 与AI对话的集成
```

### 第13-14周: 系统集成与测试
```
集成工作:
- 前后端API联调
- AI服务集成测试
- 数据流端到端验证
- 性能优化

质量保证:
- 单元测试覆盖
- 集成测试验证
- UI/UX测试
- 安全性测试
```

### 第15-16周: 内测版本发布
```
发布准备:
- 生产环境部署
- 监控和日志系统
- 错误追踪设置
- 用户反馈收集机制

内测管理:
- 内测用户招募 (20-30人)
- 使用数据收集
- 反馈问题修复
- 功能优化迭代
```

---

## 🛠️ 技术架构详细设计

### 系统整体架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Flutter App   │    │   Web Portal    │    │  Admin Panel    │
│  (iOS/Android)  │    │   (Optional)    │    │   (Internal)    │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   Load Balancer │
                    │    (Nginx)      │
                    └─────────┬───────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
    │   Scala API     │ │  AI Service     │ │  Data Service   │
    │   Gateway       │ │  (Python)       │ │   (Python)      │
    │  (ZIO + Tapir)  │ │ (FastAPI+LLM)   │ │ (Data Pipeline) │
    └─────────┬───────┘ └─────────┬───────┘ └─────────┬───────┘
              │                   │                   │
              └───────┬───────────┼───────────────────┘
                      │           │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
    ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
    │  PostgreSQL     │ │     Redis       │ │  External APIs  │
    │ (User Data &    │ │   (Cache &      │ │ (Market Data &  │
    │  Portfolios)    │ │   Sessions)     │ │  News Sources)  │
    └─────────────────┘ └─────────────────┘ └─────────────────┘
```

### 服务详细设计

#### Scala API Gateway
**职责**: 用户管理、投资组合管理、请求路由、认证授权
**技术栈**: Scala 3 + ZIO 2.x + Tapir + Quill
**关键模块**:
```scala
// 用户管理服务
UserService: 用户CRUD、认证、授权
PortfolioService: 投资组合管理、持仓计算
AuthService: JWT生成验证、会话管理
NotificationService: 推送通知管理

// API路由定义  
AuthEndpoints: /api/auth/* (登录、注册、刷新令牌)
UserEndpoints: /api/user/* (用户资料、偏好设置)
PortfolioEndpoints: /api/portfolio/* (组合管理、持仓操作)
AIEndpoints: /api/ai/* (AI对话接口代理)
```

#### Python AI Service
**职责**: AI对话处理、个性化分析、智能推荐
**技术栈**: FastAPI + OpenAI/Claude API + Pandas + NumPy
**关键模块**:
```python
# AI对话管理
ChatManager: 对话会话管理、上下文维护
PersonalityEngine: 个性化交互风格适配
KnowledgeBase: 投资知识库查询和检索

# 分析引擎
PortfolioAnalyzer: 投资组合风险和性能分析
MarketAnalyzer: 市场数据分析和洞察生成
PersonalizationEngine: 个性化分析和推荐

# 外部集成
LLMService: 大语言模型API封装
DataFetcher: 市场数据获取和清洗
```

#### Python Data Service  
**职责**: 市场数据获取、数据处理、实时更新
**技术栈**: FastAPI + Pandas + APScheduler + Redis
**关键模块**:
```python
# 数据获取
MarketDataFetcher: 股价、财务数据获取
NewsDataFetcher: 财经新闻和公告获取  
DataValidator: 数据质量验证和清洗

# 数据处理
PriceCalculator: 实时价格计算和更新
RiskCalculator: 风险指标计算
TrendAnalyzer: 趋势和技术指标分析

# 缓存管理
RedisManager: 数据缓存和过期管理
DataSyncer: 数据库和缓存同步
```

### 数据库设计

#### PostgreSQL Schema (主要表结构)
```sql
-- 用户表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    risk_tolerance VARCHAR(20) DEFAULT 'moderate',
    investment_experience VARCHAR(20) DEFAULT 'beginner', 
    investment_goals TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 投资组合表
CREATE TABLE portfolios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL DEFAULT 'My Portfolio',
    total_value DECIMAL(15,2),
    total_cost DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 持仓表
CREATE TABLE holdings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID REFERENCES portfolios(id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL,
    quantity DECIMAL(15,4) NOT NULL,
    average_cost DECIMAL(10,4) NOT NULL,
    current_price DECIMAL(10,4),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 对话历史表  
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    session_title VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL, -- 'user' or 'assistant'
    content TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户偏好表
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    notification_frequency VARCHAR(20) DEFAULT 'normal',
    ai_personality VARCHAR(20) DEFAULT 'balanced',
    preferred_language VARCHAR(10) DEFAULT 'zh-CN',
    communication_style VARCHAR(20) DEFAULT 'friend',
    preferences_data JSONB,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Redis缓存策略
```
# 用户会话缓存
session:{user_id} -> JWT token info (TTL: 7 days)

# 市场数据缓存  
price:{symbol} -> current price data (TTL: 15 minutes)
news:{symbol} -> latest news (TTL: 30 minutes)

# 分析结果缓存
analysis:{portfolio_id} -> analysis results (TTL: 1 hour)

# 对话上下文缓存
chat_context:{session_id} -> conversation context (TTL: 24 hours)

# 用户偏好缓存
user_prefs:{user_id} -> user preferences (TTL: 24 hours)
```

---

## 📈 成功指标和验证计划

### 产品价值验证指标
- **用户参与度**: 平均对话轮次 > 3轮，会话时长 > 2分钟
- **功能使用率**: 80%用户查看AI分析结果，60%用户输入完整投资组合  
- **用户留存**: 7天留存率 > 60%，30天留存率 > 40%
- **口碑传播**: 净推荐值(NPS) > 30，应用商店评分 > 4.0

### AI质量验证指标  
- **对话质量**: 用户评价AI回答有用性 > 70%
- **个性化效果**: A/B测试显示个性化vs通用回答的用户满意度差异 > 20%
- **分析准确性**: 用户对投资分析结果的认同度 > 60%

### 技术性能指标
- **响应速度**: AI对话响应时间 < 3秒，API响应时间 < 500ms
- **系统稳定性**: 服务可用性 > 99.5%，错误率 < 0.1%
- **数据准确性**: 股价数据延迟 < 15分钟，准确率 > 99.9%

### 内测验证计划
**内测用户**: 20-30名在美华人投资者
**测试周期**: 4周
**数据收集**:
- 用户行为数据 (使用频率、功能偏好)
- 定性反馈 (每周用户访谈)
- 定量评估 (功能评分、满意度调研)

---

## 🚀 后续发展路径

### P1阶段 (MVP后1-2个月)
- **智能主动提醒**: 基于市场事件和用户行为的主动通知
- **实时数据集成**: 完整的市场数据支持
- **用户偏好学习**: AI个性化程度的显著提升

### P2阶段 (MVP后3-6个月)  
- **券商API集成**: Interactive Brokers自动数据获取
- **高级分析功能**: 更深度的投资分析和洞察
- **华人投资社区**: 用户互动和内容分享功能

### 规模化准备 (MVP后6-12个月)
- **多券商支持**: Schwab, Fidelity等主流券商
- **中国市场数据**: A股、港股数据集成
- **企业级功能**: 投顾机构版本、API开放平台

---

**文档状态**: 开发规划确定  
**下次更新**: 基于开发进度和内测反馈更新  
**相关文档**: ai_interaction_design.md, brainstorming_session_20250824.md