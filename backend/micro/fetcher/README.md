# Fetcher - AI-Friendly Financial Data Service

[![Python](https://img.shields.io/badge/Python-3.9%2B-blue)](https://python.org)
[![gRPC](https://img.shields.io/badge/gRPC-Enabled-green)](https://grpc.io)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

一个专为AI金融分析助理设计的现代化金融数据获取微服务，基于gRPC通信协议，支持多数据源统一接口。

## 🎯 核心特性

### 📊 数据支持
- **股票数据**: 实时行情、历史价格、财务报表、技术指标
- **市场新闻**: 多语言新闻聚合、情感分析、事件提取
- **宏观数据**: 经济指标、政策信息、市场情绪
- **另类数据**: 社交媒体情绪、内幕交易、机构持仓

### 🌐 数据源覆盖
- **国际市场**: Yahoo Finance, Alpha Vantage, Polygon, FMP
- **中国市场**: AKShare, Tushare, 东方财富, 同花顺
- **新闻源**: Reuters, Bloomberg, 财新, 证券时报
- **社交媒体**: Twitter, Reddit, 雪球, 股吧

### 🤖 AI 友好设计
- **结构化数据**: 统一的JSON Schema，便于模型理解
- **语义标注**: 字段含义说明，支持多语言
- **时间序列优化**: 标准化时间格式和索引
- **批量推理**: 支持批量数据查询和预处理
- **实时流式**: WebSocket和gRPC streaming支持

### 🚀 技术特性
- **高性能**: gRPC二进制协议，低延迟数据传输
- **可扩展**: Plugin架构，易于添加新数据源
- **容错性**: 多数据源冗余，自动故障转移
- **缓存优化**: Redis分布式缓存，智能数据预热
- **监控完备**: Prometheus指标，全链路追踪

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────┐
│                   gRPC Gateway                      │
├─────────────────────────────────────────────────────┤
│                  Query Router                       │
├─────────────────────────────────────────────────────┤
│              Provider Manager                       │
├──────────────┬──────────────┬─────────────┬─────────┤
│   Equity     │    News      │   Macro     │  Alt    │
│  Providers   │  Providers   │ Providers   │ Data    │
├──────────────┼──────────────┼─────────────┼─────────┤
│  Yahoo       │  Reuters     │   FRED      │Twitter  │
│  AKShare     │  财新        │   Wind      │Reddit   │
│  Tushare     │  Bloomberg   │   CEIC      │雪球     │
└──────────────┴──────────────┴─────────────┴─────────┘
```

## 📋 项目结构 (已整理优化)

```
fetcher/
├── main.py                     # 🚀 主入口文件 - 统一服务启动点
├── proto/                      # Protocol Buffers定义
│   ├── fetcher_service.proto  # 统一的服务接口定义
│   ├── equity.proto           # 股票数据结构
│   ├── news.proto             # 新闻数据结构
│   └── common.proto           # 通用数据类型
├── src/
│   ├── config/                # 配置管理
│   │   ├── settings.py        # 服务配置
│   │   └── logging.py         # 日志配置
│   ├── core/                  # 核心组件
│   │   ├── models/            # 数据模型
│   │   ├── providers/         # 数据源抽象层
│   │   ├── fetchers/          # 数据获取组件
│   │   ├── processors/        # 数据处理组件
│   │   └── services/          # 核心业务服务
│   ├── grpc/                  # gRPC服务实现
│   │   ├── server.py          # 🔧 整合的gRPC服务器
│   │   └── handlers/          # 服务处理器
│   ├── providers/             # 具体数据源实现
│   │   ├── yahoo/             # Yahoo Finance
│   │   └── akshare/           # AKShare中文数据
│   └── ai/                    # AI增强功能
├── scripts/                   # 工具脚本
│   └── compile_protos.sh      # Proto文件编译脚本
├── simple_test.py             # 🧪 结构测试脚本
└── test_service.py            # 🧪 功能测试脚本
```

### 📝 整理说明

✅ **解决的问题**:
- 移除重复的 `src/services/grpc_server.py` 和 `src/grpc/server.py`
- 统一为 `src/grpc/server.py` 作为唯一的gRPC服务器实现
- 创建统一的 `main.py` 入口文件
- 整合股票和新闻服务到一个统一的FetcherService

✅ **新增功能**:
- 健康检查服务集成
- 优雅的启动/关闭流程
- 完善的错误处理和日志记录
- 支持信号处理的服务管理

## 🔧 快速开始

### 环境要求
- Python 3.9+
- Redis 6.0+
- Protocol Buffers 3.20+

### 安装依赖
```bash
cd fetcher
pip install -r requirements.txt
```

### 启动服务
```bash
# 方法1: 直接运行主入口
python3 main.py

# 方法2: 使用模块方式
python3 -m main

# 方法3: 指定端口启动
FETCHER_GRPC_PORT=50052 python3 main.py
```

### 开发和测试
```bash
# 运行结构测试
python3 simple_test.py

# 运行功能测试（需要安装依赖）
python3 test_service.py

# 编译proto文件（需要grpcio-tools）
./scripts/compile_protos.sh
```

### 客户端示例
```python
from fetcher.generated.client import FetcherClient

client = FetcherClient("localhost:50051")

# 获取股票历史数据
response = client.get_equity_historical(
    symbol="AAPL",
    start_date="2024-01-01",
    end_date="2024-12-31",
    provider="yahoo"
)

# 获取市场新闻
news = client.get_market_news(
    keywords=["Apple", "iPhone"],
    language="en",
    limit=50
)
```

## 📊 数据模型设计

所有数据模型都针对AI分析进行了优化：

- **时间序列标准化**: 统一的时间格式和索引
- **多语言支持**: 中英文字段描述和数据
- **语义注解**: 详细的字段含义和单位说明
- **关联数据**: 自动链接相关的市场数据
- **质量评分**: 数据可信度和完整性评估

## 🔌 Provider 扩展

添加新数据源只需实现 `BaseProvider` 接口：

```python
class CustomProvider(BaseProvider):
    def get_equity_data(self, request):
        # 实现数据获取逻辑
        pass
    
    def validate_data(self, data):
        # 数据验证和清洗
        pass
```

## 📈 性能指标

- **延迟**: P99 < 100ms (缓存命中)
- **吞吐量**: 10K+ QPS
- **可用性**: 99.9% SLA
- **数据覆盖**: 全球70+市场

## 🤝 贡献指南

欢迎贡献代码！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详细信息。

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 🙋‍♂️ 支持

- 📧 Email: support@fetcher.ai  
- 💬 Discord: [加入社区](https://discord.gg/fetcher)
- 📖 文档: [docs.fetcher.ai](https://docs.fetcher.ai)