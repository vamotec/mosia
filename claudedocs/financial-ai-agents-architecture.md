# 金融投资AI Agents微服务架构设计

## 系统概览

基于现有Scala协调层和Python微服务架构，设计专业金融投资AI Agents系统，通过7个专业分析师角色提供全面投资决策支持。

```mermaid
graph TB
    subgraph "客户端层"
        App[移动App Flutter]
        Web[Web应用]
    end
    
    subgraph "网关层"
        Gateway[API Gateway]
        LB[负载均衡]
    end
    
    subgraph "协调层 - Scala主服务"
        MainAPI[Scala主服务<br/>API协调/认证]
        AgentOrch[Agent协调器]
        UserMgmt[用户管理]
        Portfolio[投资组合管理]
    end
    
    subgraph "AI Agents微服务层 - Python"
        FundAgent[基本面分析师<br/>fundamental-analyst]
        TechAgent[市场分析师<br/>technical-analyst] 
        RiskAgent[风险分析师<br/>risk-analyst]
        NewsAgent[新闻情绪分析师<br/>sentiment-analyst]
        MacroAgent[宏观数据分析师<br/>macro-analyst]
        TradeAgent[交易助理<br/>trade-assistant]
        ChatAgent[AI互动助理<br/>chat-assistant]
    end
    
    subgraph "数据服务层"
        Fetcher[Fetcher微服务<br/>数据获取]
        RealTime[实时数据流]
        External[外部金融API]
    end
    
    subgraph "基础设施层"
        PostgreSQL[(PostgreSQL<br/>核心数据)]
        Redis[(Redis<br/>缓存/会话)]
        Kafka[(Kafka<br/>事件流)]
        TS[(TimeSeries DB<br/>金融时序数据)]
    end
    
    subgraph "监控层"
        Metrics[指标收集]
        Alerts[告警系统]
        Logs[日志聚合]
    end
    
    App --> Gateway
    Web --> Gateway
    Gateway --> LB
    LB --> MainAPI
    
    MainAPI --> AgentOrch
    AgentOrch --> FundAgent
    AgentOrch --> TechAgent
    AgentOrch --> RiskAgent
    AgentOrch --> NewsAgent
    AgentOrch --> MacroAgent
    AgentOrch --> TradeAgent
    AgentOrch --> ChatAgent
    
    FundAgent --> Fetcher
    TechAgent --> Fetcher
    RiskAgent --> Fetcher
    NewsAgent --> Fetcher
    MacroAgent --> Fetcher
    
    Fetcher --> External
    Fetcher --> RealTime
    
    MainAPI --> PostgreSQL
    AgentOrch --> Redis
    AgentOrch --> Kafka
    Fetcher --> TS
    
    FundAgent --> Redis
    TechAgent --> TS
    RiskAgent --> PostgreSQL
    NewsAgent --> Kafka
    
    Metrics --> MainAPI
    Metrics --> FundAgent
    Alerts --> Kafka
    Logs --> MainAPI
```

## AI Agents服务拆分方案

### 1. 基本面分析师 (Fundamental Analyst Service)

**责任范围**：公司财务数据分析、行业对比、估值评价

```python
# 服务端口：9101
# 容器名：mosia-fundamental-analyst
# 数据源：财务报表、行业数据、公司公告
```

**核心功能**：
- 财务指标计算（P/E、ROE、债务比率等）
- 同行业对比分析
- 公司基本面评级
- 长期投资价值评估
- 财务健康度分析

**数据依赖**：
- 公司财务数据（季报、年报）
- 行业基准数据
- 宏观经济指标
- 公司治理信息

### 2. 市场分析师 (Technical Analyst Service)

**责任范围**：技术分析、价格趋势、图表模式识别

```python
# 服务端口：9102  
# 容器名：mosia-technical-analyst
# 数据源：实时价格、历史K线、成交量、技术指标
```

**核心功能**：
- 技术指标计算（MA、MACD、RSI、布林带）
- 图表模式识别（头肩顶、三角形、楔形）
- 趋势分析和预测
- 支撑阻力位识别
- 交易信号生成

**数据依赖**：
- 实时股价数据
- 历史价格序列
- 成交量数据
- 期权数据
- 市场深度数据

### 3. 风险分析师 (Risk Analyst Service)

**责任范围**：投资风险评估、组合优化、风险模型

```python
# 服务端口：9103
# 容器名：mosia-risk-analyst  
# 数据源：历史价格、相关性数据、波动率、Beta值
```

**核心功能**：
- VaR（风险价值）计算
- 投资组合风险分析
- 相关性分析
- 压力测试模拟
- 最优资产配置建议
- 止损止盈策略

**数据依赖**：
- 历史价格波动数据
- 资产相关性矩阵
- 宏观风险因子
- 流动性指标
- 用户风险偏好

### 4. 新闻情绪分析师 (Sentiment Analyst Service)

**责任范围**：新闻情绪分析、市场情绪量化、事件影响评估

```python
# 服务端口：9104
# 容器名：mosia-sentiment-analyst
# 数据源：财经新闻、社交媒体、公告、分析师报告
```

**核心功能**：
- 新闻情绪量化分析
- 社交媒体情绪监控
- 重大事件影响评估
- 市场恐慌/贪婪指数
- 舆论热点追踪

**数据依赖**：
- 实时财经新闻
- 社交媒体数据
- 分析师研报
- 公司公告
- 监管信息

### 5. 宏观数据分析师 (Macro Analyst Service)

**责任范围**：宏观经济分析、政策影响评估、周期性分析

```python
# 服务端口：9105
# 容器名：mosia-macro-analyst
# 数据源：经济指标、央行政策、政府数据、国际市场
```

**核心功能**：
- 经济指标分析（GDP、CPI、失业率）
- 货币政策影响评估
- 行业周期性分析
- 国际市场联动分析
- 汇率影响评估

**数据依赖**：
- 宏观经济数据
- 央行决议和政策
- 政府统计数据
- 国际经济指标
- 大宗商品价格

### 6. 交易助理 (Trade Assistant Service)

**责任范围**：交易执行支持、订单管理、执行策略优化

```python
# 服务端口：9106
# 容器名：mosia-trade-assistant
# 数据源：实时报价、订单簿、执行数据、市场微观结构
```

**核心功能**：
- 最优执行时间建议
- 订单执行策略优化
- 滑点成本预估
- 流动性分析
- 交易成本优化
- 仓位管理建议

**数据依赖**：
- 实时买卖盘数据
- 历史执行数据
- 流动性指标
- 交易成本数据
- 市场微观结构

### 7. AI互动助理 (Chat Assistant Service)

**责任范围**：个性化对话、投资建议整合、用户交互优化

```python
# 服务端口：9107
# 容器名：mosia-chat-assistant
# 数据源：用户偏好、历史对话、综合分析结果
```

**核心功能**：
- 个性化投资对话
- 多agent结果整合
- 投资建议解释
- 学习进度追踪
- 主动投资提醒

**数据依赖**：
- 用户画像数据
- 历史交互记录
- 其他agents分析结果
- 个性化偏好
- 学习行为数据

## gRPC接口设计

### 金融AI Agents服务接口

```protobuf
syntax = "proto3";

package financial_agents;

option java_multiple_files = true;
option java_package = "app.mosia.grpc.financial";
option py_generic_services = true;

import "google/protobuf/timestamp.proto";
import "google/protobuf/any.proto";

// 金融AI Agents主服务
service FinancialAgentsService {
  // 基本面分析
  rpc AnalyzeFundamentals(FundamentalAnalysisRequest) returns (FundamentalAnalysisResponse);
  
  // 技术分析
  rpc AnalyzeTechnical(TechnicalAnalysisRequest) returns (TechnicalAnalysisResponse);
  
  // 风险分析
  rpc AnalyzeRisk(RiskAnalysisRequest) returns (RiskAnalysisResponse);
  
  // 情绪分析
  rpc AnalyzeSentiment(SentimentAnalysisRequest) returns (SentimentAnalysisResponse);
  
  // 宏观分析
  rpc AnalyzeMacro(MacroAnalysisRequest) returns (MacroAnalysisResponse);
  
  // 交易助理
  rpc GetTradeAdvice(TradeAdviceRequest) returns (TradeAdviceResponse);
  
  // 综合投资建议
  rpc GetInvestmentAdvice(InvestmentAdviceRequest) returns (InvestmentAdviceResponse);
  
  // 个性化聊天
  rpc ProcessInvestmentChat(InvestmentChatRequest) returns (InvestmentChatResponse);
  
  // 实时推荐
  rpc GetRealTimeRecommendations(RealtimeRecommendationRequest) returns (stream RecommendationUpdate);
  
  // 投资组合分析
  rpc AnalyzePortfolio(PortfolioAnalysisRequest) returns (PortfolioAnalysisResponse);
  
  // 健康检查
  rpc HealthCheck(HealthCheckRequest) returns (HealthCheckResponse);
}

// 基本面分析请求
message FundamentalAnalysisRequest {
  string user_id = 1;
  repeated string symbols = 2;  // 股票代码列表
  string analysis_depth = 3;    // quick|standard|deep
  repeated string metrics = 4;  // 指定要分析的指标
  TimeRange time_range = 5;
  AnalysisOptions options = 6;
}

message FundamentalAnalysisResponse {
  repeated FundamentalAnalysis results = 1;
  string overall_assessment = 2;
  repeated InvestmentRecommendation recommendations = 3;
  AnalysisMetadata metadata = 4;
}

message FundamentalAnalysis {
  string symbol = 1;
  string company_name = 2;
  
  // 财务指标
  FinancialMetrics financial_metrics = 3;
  
  // 行业对比
  IndustryComparison industry_comparison = 4;
  
  // 估值分析
  ValuationAnalysis valuation = 5;
  
  // 质量评分
  QualityScore quality_score = 6;
  
  // 分析结论
  string analysis_summary = 7;
  InvestmentRating rating = 8;
  
  google.protobuf.Timestamp analyzed_at = 9;
}

message FinancialMetrics {
  double pe_ratio = 1;
  double pb_ratio = 2;
  double roe = 3;
  double debt_ratio = 4;
  double current_ratio = 5;
  double gross_margin = 6;
  double net_margin = 7;
  double revenue_growth = 8;
  double earnings_growth = 9;
}

message IndustryComparison {
  string industry = 1;
  double industry_avg_pe = 2;
  double relative_performance = 3;
  int32 industry_ranking = 4;
  int32 total_companies = 5;
}

message ValuationAnalysis {
  double fair_value = 1;
  double current_price = 2;
  double upside_potential = 3;
  string valuation_method = 4;
  double confidence_level = 5;
}

message QualityScore {
  double overall_score = 1;        // 0-100
  double financial_strength = 2;
  double management_quality = 3;
  double competitive_advantage = 4;
  double growth_prospects = 5;
}

// 技术分析请求
message TechnicalAnalysisRequest {
  string user_id = 1;
  repeated string symbols = 2;
  string timeframe = 3;         // 1m|5m|15m|1h|1d|1w
  repeated string indicators = 4; // ma|macd|rsi|bollinger|fibonacci
  TimeRange time_range = 5;
  AnalysisOptions options = 6;
}

message TechnicalAnalysisResponse {
  repeated TechnicalAnalysis results = 1;
  MarketTrend overall_trend = 2;
  repeated TradingSignal signals = 3;
  AnalysisMetadata metadata = 4;
}

message TechnicalAnalysis {
  string symbol = 1;
  double current_price = 2;
  
  // 技术指标
  map<string, TechnicalIndicator> indicators = 3;
  
  // 图表模式
  repeated ChartPattern patterns = 4;
  
  // 支撑阻力
  SupportResistance support_resistance = 5;
  
  // 趋势分析
  TrendAnalysis trend = 6;
  
  // 交易信号
  repeated TradingSignal signals = 7;
  
  google.protobuf.Timestamp analyzed_at = 8;
}

message TechnicalIndicator {
  string name = 1;
  double value = 2;
  string signal = 3;  // BUY|SELL|HOLD
  double confidence = 4;
  map<string, double> parameters = 5;
}

message ChartPattern {
  string pattern_type = 1;  // head_shoulders|triangle|flag|wedge
  string direction = 2;     // bullish|bearish|neutral
  double confidence = 3;
  PriceLevel breakout_level = 4;
  PriceLevel target_level = 5;
}

message SupportResistance {
  repeated PriceLevel support_levels = 1;
  repeated PriceLevel resistance_levels = 2;
  PriceLevel key_support = 3;
  PriceLevel key_resistance = 4;
}

message PriceLevel {
  double price = 1;
  double strength = 2;  // 0-1
  int32 touch_count = 3;
  google.protobuf.Timestamp last_touch = 4;
}

// 风险分析请求
message RiskAnalysisRequest {
  string user_id = 1;
  Portfolio portfolio = 2;
  string risk_model = 3;      // var|cvar|montecarlo|stress
  double confidence_level = 4; // 0.95, 0.99
  int32 time_horizon_days = 5;
  AnalysisOptions options = 6;
}

message RiskAnalysisResponse {
  PortfolioRisk portfolio_risk = 1;
  repeated AssetRisk individual_risks = 2;
  repeated RiskScenario scenarios = 3;
  OptimizationSuggestion optimization = 4;
  AnalysisMetadata metadata = 5;
}

message Portfolio {
  string portfolio_id = 1;
  repeated Position positions = 2;
  double total_value = 3;
  string base_currency = 4;
}

message Position {
  string symbol = 1;
  double quantity = 2;
  double current_price = 3;
  double cost_basis = 4;
  double market_value = 5;
  double weight = 6;  // 在组合中的权重
}

message PortfolioRisk {
  double var_1d = 1;           // 1日风险价值
  double var_10d = 2;          // 10日风险价值
  double cvar = 3;             // 条件风险价值
  double sharpe_ratio = 4;
  double max_drawdown = 5;
  double volatility = 6;
  double beta = 7;
  CorrelationMatrix correlation = 8;
}

message AssetRisk {
  string symbol = 1;
  double individual_var = 2;
  double contribution_to_var = 3;
  double beta = 4;
  double volatility = 5;
  double liquidity_risk = 6;
  RiskRating risk_rating = 7;
}

message RiskScenario {
  string scenario_name = 1;    // market_crash|rate_hike|recession
  double probability = 2;
  double portfolio_impact = 3; // 百分比影响
  repeated AssetImpact asset_impacts = 4;
}

// 投资建议
message InvestmentRecommendation {
  string symbol = 1;
  RecommendationAction action = 2;  // BUY|SELL|HOLD
  double target_price = 3;
  double confidence = 4;
  string reasoning = 5;
  repeated string supporting_factors = 6;
  repeated string risk_factors = 7;
  TimeFrame time_frame = 8;
}

// 通用类型定义
message TimeRange {
  google.protobuf.Timestamp start_time = 1;
  google.protobuf.Timestamp end_time = 2;
}

message AnalysisOptions {
  bool include_charts = 1;
  bool use_ml_models = 2;
  string language = 3;         // zh|en
  repeated string exclude_factors = 4;
  map<string, string> custom_params = 5;
}

message AnalysisMetadata {
  string analysis_id = 1;
  repeated string data_sources = 2;
  int32 processing_time_ms = 3;
  string model_version = 4;
  double data_quality_score = 5;
  google.protobuf.Timestamp generated_at = 6;
}

enum RecommendationAction {
  STRONG_BUY = 0;
  BUY = 1;
  HOLD = 2;
  SELL = 3;
  STRONG_SELL = 4;
}

enum TimeFrame {
  SHORT_TERM = 0;   // 1-30天
  MEDIUM_TERM = 1;  // 1-6月
  LONG_TERM = 2;    // 6月-2年
}

enum RiskRating {
  VERY_LOW = 0;
  LOW = 1;
  MEDIUM = 2;
  HIGH = 3;
  VERY_HIGH = 4;
}

message InvestmentRating {
  double score = 1;            // 0-100
  RecommendationAction action = 2;
  double target_price = 3;
  double price_range_low = 4;
  double price_range_high = 5;
  TimeFrame time_horizon = 6;
}

// 实时推荐更新
message RecommendationUpdate {
  string update_id = 1;
  string update_type = 2;      // price_alert|news_event|signal|recommendation
  repeated string affected_symbols = 3;
  string message = 4;
  Priority priority = 5;
  google.protobuf.Timestamp timestamp = 6;
  map<string, string> metadata = 7;
}

enum Priority {
  LOW = 0;
  NORMAL = 1;
  HIGH = 2;
  URGENT = 3;
}
```

## 数据流设计

### 1. 实时数据流架构

```mermaid
sequenceDiagram
    participant App as 移动App
    participant Scala as Scala协调层
    participant Agents as AI Agents
    participant Fetcher as Fetcher服务
    participant External as 外部API
    participant Kafka as Kafka流
    participant Redis as Redis缓存

    External->>Fetcher: 实时价格推送
    Fetcher->>Kafka: 发布价格事件
    Kafka->>TechAgent: 技术分析触发
    Kafka->>RiskAgent: 风险重算触发
    
    TechAgent->>Redis: 更新技术指标
    RiskAgent->>Redis: 更新风险指标
    
    App->>Scala: 请求投资建议
    Scala->>Agents: 协调多agent分析
    
    par 并行分析
        Agents->>FundAgent: 基本面分析
        Agents->>TechAgent: 技术分析  
        Agents->>RiskAgent: 风险分析
        Agents->>NewsAgent: 情绪分析
    end
    
    Agents->>ChatAgent: 整合分析结果
    ChatAgent->>Scala: 返回个性化建议
    Scala->>App: 投资建议响应
```

### 2. 数据同步策略

**实时数据（毫秒级）**：
- 股价变动 → 技术分析师、风险分析师
- 订单簿变化 → 交易助理
- 新闻发布 → 新闻情绪分析师

**准实时数据（分钟级）**：
- 财经新闻 → 情绪分析师
- 社交媒体情绪 → 情绪分析师
- 技术指标更新 → 交易助理

**定期数据（小时/日级）**：
- 财务数据 → 基本面分析师
- 宏观数据 → 宏观分析师
- 研究报告 → 基本面分析师

### 3. 缓存策略设计

```yaml
缓存层设计:
  Redis热点数据:
    - 实时价格: TTL 30s
    - 技术指标: TTL 5min
    - 用户偏好: TTL 1h
    - 分析结果: TTL 15min
    
  PostgreSQL持久数据:
    - 用户投资组合
    - 历史分析记录
    - 投资决策历史
    - 风险偏好设定
    
  TimeSeries数据库:
    - 历史价格序列
    - 技术指标时序
    - 波动率历史
    - 相关性矩阵
```

## 服务间通信设计

### 1. Agent协调模式

```python
# Scala Agent协调器设计
class FinancialAgentOrchestrator {
  // 并行分析协调
  async def getComprehensiveAnalysis(request: AnalysisRequest): Future[AnalysisResult] = {
    val futures = Seq(
      fundamentalAnalyst.analyze(request.symbols),
      technicalAnalyst.analyze(request.symbols), 
      riskAnalyst.analyze(request.portfolio),
      sentimentAnalyst.analyze(request.symbols),
      macroAnalyst.analyze(request.context)
    )
    
    // 等待所有分析完成
    val results = await Future.sequence(futures)
    
    // 通过聊天助理整合结果
    chatAssistant.synthesize(results, request.userPreferences)
  }
}
```

### 2. 事件驱动更新

```yaml
Kafka事件流设计:
  topics:
    - financial.prices.realtime     # 实时价格
    - financial.news.updates        # 新闻更新  
    - financial.analysis.results    # 分析结果
    - user.portfolio.changes        # 组合变更
    - market.alerts                 # 市场告警
    
  消费者组:
    - technical-analyst-group       # 技术分析消费价格
    - risk-analyst-group           # 风险分析消费组合变更
    - sentiment-analyst-group      # 情绪分析消费新闻
    - notification-service-group   # 通知服务消费告警
```

## 数据模型设计

### 1. 核心领域模型

```python
# 投资标的模型
class FinancialInstrument:
    symbol: str
    name: str
    instrument_type: InstrumentType  # STOCK|BOND|ETF|FUTURE|OPTION
    exchange: str
    currency: str
    sector: str
    industry: str
    market_cap: Optional[float]
    
# 用户投资画像
class InvestorProfile:
    user_id: str
    risk_tolerance: RiskTolerance  # CONSERVATIVE|MODERATE|AGGRESSIVE
    investment_horizon: TimeHorizon # SHORT|MEDIUM|LONG
    investment_goals: List[InvestmentGoal]
    preferred_sectors: List[str]
    excluded_sectors: List[str]
    max_single_position: float   # 单一持仓比例限制
    liquidity_needs: float       # 流动性需求
    
# 投资组合模型  
class Portfolio:
    portfolio_id: str
    user_id: str
    name: str
    positions: List[Position]
    cash_balance: float
    total_value: float
    target_allocation: Dict[str, float]  # 目标配置比例
    rebalance_threshold: float
    
# 分析结果模型
class AnalysisResult:
    analysis_id: str
    symbol: str
    analyst_type: AnalystType
    analysis_data: Dict[str, Any]
    confidence: float
    generated_at: datetime
    valid_until: datetime
    data_sources: List[str]
```

### 2. 数据库schema设计

```sql
-- 投资标的表
CREATE TABLE financial_instruments (
    symbol VARCHAR(20) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    instrument_type VARCHAR(20) NOT NULL,
    exchange VARCHAR(50),
    currency VARCHAR(3),
    sector VARCHAR(50),
    industry VARCHAR(100),
    market_cap BIGINT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- 投资组合表
CREATE TABLE portfolios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    total_value DECIMAL(20,2),
    cash_balance DECIMAL(20,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- 持仓表
CREATE TABLE positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id),
    symbol VARCHAR(20) NOT NULL REFERENCES financial_instruments(symbol),
    quantity DECIMAL(20,8),
    avg_cost DECIMAL(20,8),
    current_price DECIMAL(20,8),
    market_value DECIMAL(20,2),
    unrealized_pnl DECIMAL(20,2),
    weight DECIMAL(5,4), -- 权重百分比
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- 分析结果表
CREATE TABLE analysis_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    symbol VARCHAR(20) REFERENCES financial_instruments(symbol),
    analyst_type VARCHAR(50) NOT NULL,
    analysis_data JSONB NOT NULL,
    confidence DECIMAL(3,2),
    generated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    valid_until TIMESTAMP WITH TIME ZONE,
    data_sources TEXT[],
    INDEX idx_analysis_user_symbol (user_id, symbol),
    INDEX idx_analysis_type_time (analyst_type, generated_at),
    INDEX idx_analysis_valid (valid_until) WHERE valid_until > now()
);

-- 投资建议表
CREATE TABLE investment_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    symbol VARCHAR(20) NOT NULL REFERENCES financial_instruments(symbol),
    recommendation_action VARCHAR(20) NOT NULL, -- BUY|SELL|HOLD
    target_price DECIMAL(20,8),
    confidence DECIMAL(3,2),
    reasoning TEXT,
    supporting_factors TEXT[],
    risk_factors TEXT[],
    time_frame VARCHAR(20),
    analyst_sources TEXT[], -- 哪些分析师参与了建议
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true
);

-- 用户投资偏好表
CREATE TABLE investor_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    risk_tolerance VARCHAR(20) NOT NULL DEFAULT 'MODERATE',
    investment_horizon VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    investment_goals JSONB,
    preferred_sectors TEXT[],
    excluded_sectors TEXT[],
    max_single_position DECIMAL(3,2) DEFAULT 0.10, -- 10%
    liquidity_needs DECIMAL(3,2) DEFAULT 0.05,     -- 5%
    rebalance_threshold DECIMAL(3,2) DEFAULT 0.05,  -- 5%
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
```

## 个性化机制设计

### 1. 用户画像学习

```python
class UserPersonalizationEngine:
    async def learn_from_interactions(self, user_id: str, interaction_data: Dict):
        """从用户交互中学习偏好"""
        # 分析用户行为模式
        patterns = await self._extract_behavior_patterns(interaction_data)
        
        # 更新风险偏好
        await self._update_risk_tolerance(user_id, patterns)
        
        # 优化推荐权重
        await self._adjust_recommendation_weights(user_id, patterns)
        
        # 学习交互风格
        await self._learn_communication_style(user_id, patterns)
    
    async def _extract_behavior_patterns(self, data: Dict) -> BehaviorPatterns:
        return BehaviorPatterns(
            click_through_rate=self._calculate_ctr(data),
            preferred_analysis_depth=self._infer_depth_preference(data),
            sector_preferences=self._extract_sector_preferences(data),
            time_preference=self._extract_time_preferences(data),
            risk_behavior=self._analyze_risk_behavior(data)
        )
```

### 2. 动态推荐优化

```python
class RecommendationOptimizer:
    def __init__(self):
        self.user_feedback_weights = {}
        self.success_rate_by_analyst = {}
        
    async def optimize_recommendations(
        self, 
        user_id: str, 
        base_recommendations: List[Recommendation]
    ) -> List[Recommendation]:
        """基于用户反馈优化推荐"""
        user_weights = await self._get_user_weights(user_id)
        
        # 重新计算置信度
        for rec in base_recommendations:
            analyst_success_rate = self.success_rate_by_analyst.get(
                rec.metadata.get('analyst_type'), 1.0
            )
            user_preference_weight = user_weights.get(
                rec.metadata.get('category'), 1.0
            )
            
            # 动态调整置信度
            rec.confidence = min(1.0, 
                rec.confidence * analyst_success_rate * user_preference_weight
            )
        
        return sorted(base_recommendations, key=lambda x: x.confidence, reverse=True)
```

## 扩展性架构设计

### 1. 新Agent角色添加机制

```python
# Agent注册机制
class AgentRegistry:
    def __init__(self):
        self.registered_agents = {}
        self.agent_capabilities = {}
        
    def register_agent(self, agent_spec: AgentSpecification):
        """动态注册新的分析师agent"""
        self.registered_agents[agent_spec.name] = agent_spec
        self.agent_capabilities[agent_spec.name] = agent_spec.capabilities
        
        # 更新路由规则
        self._update_routing_rules(agent_spec)
        
        # 注册health check
        self._register_health_check(agent_spec)

# Agent规范定义
@dataclass
class AgentSpecification:
    name: str
    description: str
    capabilities: List[str]
    input_types: List[str]
    output_types: List[str]
    dependencies: List[str]
    grpc_port: int
    health_check_endpoint: str
    metrics_config: Dict
```

### 2. 水平扩展机制

```yaml
# 负载均衡配置
agents_scaling:
  fundamental_analyst:
    min_instances: 2
    max_instances: 8
    cpu_threshold: 70%
    memory_threshold: 80%
    scaling_metrics:
      - request_rate
      - analysis_queue_length
      
  technical_analyst:
    min_instances: 3  # 技术分析请求量大
    max_instances: 12
    cpu_threshold: 75%
    real_time_priority: true
    
  risk_analyst:
    min_instances: 2
    max_instances: 6
    memory_intensive: true
    batch_processing: true
```

## 实时性方案设计

### 1. 实时数据处理pipeline

```python
class RealTimeProcessor:
    def __init__(self):
        self.price_stream = KafkaProducer("financial.prices.realtime")
        self.analysis_trigger = KafkaProducer("analysis.triggers")
        
    async def process_price_update(self, price_data: PriceUpdate):
        """处理实时价格更新"""
        # 1. 验证数据质量
        if not self._validate_price_data(price_data):
            return
            
        # 2. 更新缓存
        await self._update_price_cache(price_data)
        
        # 3. 触发相关分析
        if self._is_significant_change(price_data):
            await self._trigger_technical_analysis(price_data.symbol)
            await self._trigger_risk_rebalance(price_data.symbol)
            
        # 4. 检查用户告警条件
        await self._check_user_alerts(price_data)
```

### 2. 推送通知机制

```python
class NotificationEngine:
    async def process_analysis_result(self, result: AnalysisResult):
        """分析结果智能推送"""
        # 获取关注该标的的用户
        interested_users = await self._get_interested_users(result.symbol)
        
        for user_id in interested_users:
            # 个性化通知内容
            notification = await self._personalize_notification(user_id, result)
            
            # 选择推送渠道
            channel = await self._select_notification_channel(user_id, notification.priority)
            
            # 异步推送
            await self._send_notification(user_id, notification, channel)
```

## Docker微服务配置

### 1. 完整docker-compose配置

```yaml
version: '3.8'

networks:
  financial-network:
    driver: bridge

services:
  # 基本面分析师
  fundamental-analyst:
    build:
      context: ./agents/fundamental_analyst
      dockerfile: Dockerfile
    image: mosia/fundamental-analyst:latest
    container_name: mosia-fundamental-analyst
    ports:
      - "9101:50051"
      - "8101:8080"  # metrics
    environment:
      - GRPC_PORT=50051
      - ANALYST_TYPE=fundamental
      - DB_URL=postgresql://mosia:ttr851217@postgres:5432/mosia_dev
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVER=kafka:29092
      - FINANCIAL_DATA_API_KEY=${FUNDAMENTAL_API_KEY}
      - AI_MODEL_CONFIG=gpt-4-financial
      - ANALYSIS_CACHE_TTL=900  # 15分钟
      - MAX_CONCURRENT_ANALYSIS=5
    networks:
      - financial-network
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_started
    restart: unless-stopped

  # 技术分析师
  technical-analyst:
    build:
      context: ./agents/technical_analyst
      dockerfile: Dockerfile
    image: mosia/technical-analyst:latest  
    container_name: mosia-technical-analyst
    ports:
      - "9102:50052"
      - "8102:8080"
    environment:
      - GRPC_PORT=50052
      - ANALYST_TYPE=technical
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVER=kafka:29092
      - TIMESERIES_DB_HOST=timescaledb
      - TECHNICAL_INDICATORS=ma,macd,rsi,bollinger,fibonacci
      - CHART_PATTERN_MODEL=pattern-recognition-v2
      - REAL_TIME_PROCESSING=true
      - INDICATOR_CALCULATION_INTERVAL=60  # 1分钟
    networks:
      - financial-network
    restart: unless-stopped

  # 风险分析师
  risk-analyst:
    build:
      context: ./agents/risk_analyst
      dockerfile: Dockerfile
    image: mosia/risk-analyst:latest
    container_name: mosia-risk-analyst
    ports:
      - "9103:50053"
      - "8103:8080"
    environment:
      - GRPC_PORT=50053
      - ANALYST_TYPE=risk
      - DB_URL=postgresql://mosia:ttr851217@postgres:5432/mosia_dev
      - REDIS_HOST=redis
      - RISK_MODEL_TYPE=monte_carlo
      - VAR_CONFIDENCE_LEVELS=0.95,0.99
      - CORRELATION_UPDATE_INTERVAL=3600  # 1小时
      - STRESS_TEST_SCENARIOS=market_crash,interest_hike,recession
      - PORTFOLIO_REBALANCE_THRESHOLD=0.05
    networks:
      - financial-network
    restart: unless-stopped

  # 新闻情绪分析师
  sentiment-analyst:
    build:
      context: ./agents/sentiment_analyst
      dockerfile: Dockerfile
    image: mosia/sentiment-analyst:latest
    container_name: mosia-sentiment-analyst
    ports:
      - "9104:50054"
      - "8104:8080"
    environment:
      - GRPC_PORT=50054
      - ANALYST_TYPE=sentiment
      - KAFKA_BOOTSTRAP_SERVER=kafka:29092
      - NEWS_API_KEYS=${NEWS_API_KEYS}
      - SOCIAL_MEDIA_API_KEYS=${SOCIAL_API_KEYS}
      - SENTIMENT_MODEL=finbert-sentiment
      - NEWS_PROCESSING_BATCH_SIZE=50
      - SENTIMENT_UPDATE_INTERVAL=300  # 5分钟
      - LANGUAGE_SUPPORT=zh,en
    networks:
      - financial-network
    restart: unless-stopped

  # 宏观分析师
  macro-analyst:
    build:
      context: ./agents/macro_analyst
      dockerfile: Dockerfile
    image: mosia/macro-analyst:latest
    container_name: mosia-macro-analyst
    ports:
      - "9105:50055"
      - "8105:8080"
    environment:
      - GRPC_PORT=50055
      - ANALYST_TYPE=macro
      - ECONOMIC_DATA_APIS=${MACRO_API_KEYS}
      - MACRO_INDICATORS=gdp,cpi,unemployment,interest_rates
      - POLICY_ANALYSIS_MODEL=policy-impact-v1
      - UPDATE_FREQUENCY=daily
      - CORRELATION_ANALYSIS=sector,currency,commodity
    networks:
      - financial-network
    restart: unless-stopped

  # 交易助理
  trade-assistant:
    build:
      context: ./agents/trade_assistant
      dockerfile: Dockerfile
    image: mosia/trade-assistant:latest
    container_name: mosia-trade-assistant
    ports:
      - "9106:50056"  
      - "8106:8080"
    environment:
      - GRPC_PORT=50056
      - ANALYST_TYPE=trading
      - REDIS_HOST=redis
      - EXECUTION_COST_MODEL=impact-cost-v1
      - ORDER_BOOK_DEPTH=10
      - LIQUIDITY_THRESHOLD=1000000  # $1M
      - SLIPPAGE_MODEL=adaptive
      - EXECUTION_OPTIMIZATION=vwap,twap,pov
    networks:
      - financial-network
    restart: unless-stopped

  # AI聊天助理
  chat-assistant:
    build:
      context: ./agents/chat_assistant
      dockerfile: Dockerfile
    image: mosia/chat-assistant:latest
    container_name: mosia-chat-assistant
    ports:
      - "9107:50057"
      - "8107:8080"
    environment:
      - GRPC_PORT=50057
      - ANALYST_TYPE=chat
      - DB_URL=postgresql://mosia:ttr851217@postgres:5432/mosia_dev
      - REDIS_HOST=redis
      - CONVERSATION_MODEL=claude-3-sonnet
      - PERSONALITY_PROFILE=professional-financial-advisor
      - MEMORY_RETENTION_DAYS=30
      - CONTEXT_WINDOW_SIZE=4000
      - RESPONSE_STYLE=conversational,educational
    networks:
      - financial-network
    restart: unless-stopped

  # 时序数据库
  timescaledb:
    image: timescale/timescaledb:latest-pg14
    container_name: mosia-timescaledb
    ports:
      - "5433:5432"
    environment:
      - POSTGRES_DB=financial_timeseries
      - POSTGRES_USER=mosia
      - POSTGRES_PASSWORD=ttr851217
    volumes:
      - timeseries-data:/var/lib/postgresql/data
    networks:
      - financial-network
    restart: unless-stopped

volumes:
  timeseries-data:
    driver: local
```

## 实施计划和优先级

### Phase 1: 核心基础（2-3周）
**优先级：P0 - 关键路径**

1. **基础设施准备**
   - 扩展现有gRPC接口定义
   - 创建金融数据模型
   - 设置TimescaleDB时序数据库
   - 配置Kafka金融事件topics

2. **基本面分析师**（最核心）
   - 财务指标计算引擎
   - 行业对比分析
   - 基础估值模型
   - 与fetcher服务集成

3. **技术分析师**（用户最常用）
   - 基础技术指标（MA、MACD、RSI）
   - 简单图表模式识别
   - 实时价格处理
   - 交易信号生成

### Phase 2: 风险和智能化（3-4周）
**优先级：P1 - 核心竞争力**

1. **风险分析师**
   - VaR风险计算
   - 投资组合优化
   - 相关性分析
   - 基础压力测试

2. **情绪分析师**  
   - 新闻情绪量化
   - 社交媒体监控
   - 事件影响评估
   - 市场情绪指数

3. **AI聊天助理基础版**
   - 多agent结果整合
   - 基础对话能力
   - 个性化推荐

### Phase 3: 高级功能（4-5周）
**优先级：P2 - 差异化特性**

1. **宏观分析师**
   - 经济指标分析
   - 政策影响模型
   - 周期性分析
   - 国际市场联动

2. **交易助理**
   - 执行成本优化
   - 流动性分析
   - 订单策略建议
   - 仓位管理优化

3. **高级个性化**
   - 深度学习用户偏好
   - 主动推荐优化
   - 个性化风险模型

### Phase 4: 优化和扩展（3-4周）
**优先级：P3 - 性能和体验**

1. **性能优化**
   - 实时数据流优化
   - 缓存策略调优
   - 计算资源优化
   - 响应时间优化

2. **高级AI功能**
   - 深度学习模型集成
   - 复杂图表模式识别
   - 高级风险模型
   - 智能对话增强

## 技术实现要点

### 1. 数据安全和合规

```python
class ComplianceLayer:
    """金融合规层"""
    
    async def validate_data_usage(self, data_request: DataRequest) -> ComplianceResult:
        """验证数据使用合规性"""
        # 检查数据权限
        # 验证用户资质
        # 记录数据访问日志
        # 确保数据使用符合监管要求
        pass
        
    async def audit_recommendations(self, recommendations: List[Recommendation]) -> AuditResult:
        """审计投资建议合规性"""
        # 检查建议是否符合用户风险承受能力
        # 验证建议依据充分性
        # 记录建议生成过程
        # 确保免责声明完整
        pass
```

### 2. 容错和降级机制

```python
class FaultToleranceManager:
    async def handle_agent_failure(self, agent_name: str, error: Exception):
        """Agent故障处理"""
        # 1. 记录故障
        await self._log_failure(agent_name, error)
        
        # 2. 尝试降级服务
        fallback_result = await self._get_fallback_analysis(agent_name)
        
        # 3. 通知用户服务状态
        await self._notify_service_degradation(agent_name)
        
        # 4. 触发自动恢复
        await self._attempt_auto_recovery(agent_name)
        
        return fallback_result
```

### 3. 监控和告警

```yaml
监控指标:
  业务指标:
    - 分析请求成功率
    - 推荐点击率  
    - 用户满意度评分
    - 投资建议准确率
    
  技术指标:
    - gRPC请求延迟
    - Agent服务可用性
    - 数据获取成功率
    - 缓存命中率
    
  告警规则:
    - 任何Agent服务不可用 > 1分钟
    - 分析延迟 > 10秒
    - 数据获取失败率 > 5%
    - 投资建议准确率 < 70%
```

这个架构设计充分考虑了金融应用的特殊要求：数据准确性、实时性、合规性和个性化。通过7个专业AI Agent的协作，为用户提供全方位的投资决策支持，同时保持系统的可扩展性和容错能力。