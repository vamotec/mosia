# Mosia AI Microservices

AI-powered microservices for the Mosia collaborative workspace platform, providing intelligent content analysis, recommendations, and external data integration.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Mosia AI Ecosystem                       │
├─────────────────────────────────────────────────────────────┤
│  Main Service (Scala/ZIO)                                   │
│  ├─ HTTP API ←→ Frontend                                    │
│  ├─ Business Logic                                          │
│  └─ gRPC Clients ↓                                         │
├─────────────────────────────────────────────────────────────┤
│  AI Microservices (Python)                                 │
│  ├─ Agents Service (port 9091)                             │
│  │  ├─ Content Analysis                                     │
│  │  ├─ Recommendations                                      │
│  │  ├─ Content Generation                                   │
│  │  └─ Conversational AI                                    │
│  │                                                          │
│  └─ Fetcher Service (port 9092)                            │
│     ├─ External Data Fetching                               │
│     ├─ Web Scraping                                         │
│     ├─ Data Processing                                      │
│     └─ Real-time Streams                                    │
├─────────────────────────────────────────────────────────────┤
│  Infrastructure                                             │
│  ├─ PostgreSQL (shared database)                           │
│  ├─ Redis (caching)                                        │
│  └─ Kafka (event streaming)                                │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Python 3.11+ (for local development)
- Make (optional, for convenience)

### Start All Services
```bash
# Build and start all microservices
make build
make start

# Check service health
make health

# View logs
make logs
```

### Manual Setup
```bash
# Start with docker-compose
docker-compose -f docker-compose.microservices.yml up -d

# Check status
docker-compose -f docker-compose.microservices.yml ps
```

## 📦 Services

### 🤖 Agents Service (Port 9091)
**Capabilities:**
- **Content Analysis**: Document parsing, sentiment analysis, keyword extraction
- **Intelligent Recommendations**: API-driven business logic optimization, workspace patterns
- **Content Generation**: AI-powered text creation, summarization
- **Conversational AI**: Chat handling, natural language processing

**Technologies:** FastAPI, gRPC, OpenAI/Anthropic APIs, lightweight business logic

### 📡 Fetcher Service (Port 9092)
**Capabilities:**
- **External Data Integration**: REST/GraphQL API fetching with rate limiting
- **Web Scraping**: Intelligent content extraction (with/without JavaScript)
- **Data Processing**: Format conversion, validation, enrichment
- **Real-time Streams**: Webhooks, streaming data processing

**Technologies:** FastAPI, gRPC, aiohttp, BeautifulSoup, lightweight data processing

## 🔌 API Integration

### From Scala Main Service
```scala
// Inject AI service
val aiService: AIService = ???

// Analyze content
val analysis = aiService.analyzeContent(
  user = currentUser,
  workspaceId = "workspace-123",
  content = "Your content here",
  contentType = "text"
)

// Get recommendations
val recommendations = aiService.getRecommendations(
  user = currentUser,
  workspaceId = "workspace-123", 
  context = "project planning",
  limit = 10
)

// Fetch external data
val fetchResult = aiService.fetchExternalData(
  user = currentUser,
  workspaceId = "workspace-123",
  sourceType = "api",
  sourceUrl = "https://api.example.com/data"
)
```

### HTTP API Endpoints
```bash
# Content Analysis
POST /api/v1/ai/analyze
POST /api/v1/ai/keywords  
POST /api/v1/ai/sentiment

# Recommendations
GET  /api/v1/ai/recommendations?context=project&limit=10
PUT  /api/v1/ai/preferences

# Content Generation
POST /api/v1/ai/generate
POST /api/v1/ai/summarize

# Chat
POST /api/v1/ai/chat
GET  /api/v1/ai/chat/history?sessionId=123

# Data Fetching
POST /api/v1/ai/fetch
POST /api/v1/ai/process
```

## 🛠️ Development

### Local Development Setup
```bash
# Install dependencies (lightweight)
make install-deps

# Format code
make format

# Run linting
make lint

# Run tests
make test
```

### Service-Specific Commands
```bash
# Agents service
cd agents
pip install -e .[dev]
python -m src.agents.main

# Fetcher service  
cd fetcher
pip install -e .[dev]
python -m src.fetcher.main
```

### Adding New AI Features
1. **Define protobuf interface** in `proto/*.proto`
2. **Implement service logic** in `src/*/core/`
3. **Add gRPC handler** in `src/*/grpc/handlers/`
4. **Update Scala integration** in main service
5. **Add HTTP endpoints** for external access

## 📊 Monitoring

### Health Checks
```bash
# Check all services
make health

# Individual service checks
./agents/scripts/health_check.sh
./fetcher/scripts/health_check.sh
```

### Metrics & Monitoring
- **Prometheus**: Collects metrics from all services
- **Grafana**: Visualizes performance dashboards  
- **Alertmanager**: Sends alerts for issues

**Access Points:**
- Agents metrics: http://localhost:8080/metrics
- Fetcher metrics: http://localhost:8081/metrics  
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

### Key Metrics
- Request latency and throughput
- Error rates and success rates
- AI model inference times
- Resource utilization (CPU, memory)
- External API response times
- Cache hit rates

## 🔧 Configuration

### Environment Variables
Both services support extensive configuration via environment variables:

**Common:**
- `LOG_LEVEL`: Logging level (debug, info, warning, error)
- `GRPC_PORT`: gRPC server port  
- `DB_URL`: PostgreSQL connection string
- `REDIS_HOST`/`REDIS_PORT`: Redis connection
- `KAFKA_BOOTSTRAP_SERVER`: Kafka connection

**Agents Service:**
- `OPENAI_API_KEY`: OpenAI API access
- `ANTHROPIC_API_KEY`: Anthropic API access
- `ENABLE_*`: Feature flags for AI capabilities

**Fetcher Service:**
- `MAX_CONCURRENT_FETCHES`: Concurrent request limit
- `USER_AGENT`: HTTP user agent string
- `ENABLE_*`: Feature flags for fetching capabilities

### Production Configuration
- Use environment-specific `.env` files
- Configure external API keys securely
- Set appropriate resource limits
- Enable comprehensive monitoring

## 🧪 Testing

### Unit Tests
```bash
# Run all tests
make test

# Service-specific tests
cd agents && python -m pytest tests/
cd fetcher && python -m pytest tests/
```

### Integration Tests
```bash
# Start test environment
docker-compose -f docker-compose.test.yml up -d

# Run integration tests
python -m pytest tests/integration/
```

### Load Testing
```bash
# Use ghz for gRPC load testing
ghz --insecure \
  --proto ./proto/agents_service.proto \
  --call agents.AgentsService.AnalyzeContent \
  --data '{"user_id":"test","workspace_id":"test","content":"test content","content_type":"text"}' \
  localhost:9091
```

## 🚀 Deployment

### Development
```bash
make start
```

### Production  
```bash
# Build production images
docker build -t mosia/agents-service:latest ./agents
docker build -t mosia/fetcher-service:latest ./fetcher

# Deploy with production configuration
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes
```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/
```

## 🔍 Troubleshooting

### Common Issues

**Services not starting:**
```bash
# Check logs
make logs-agents
make logs-fetcher

# Check health
make health
```

**gRPC connection issues:**
```bash
# Test gRPC connectivity
grpcurl -plaintext localhost:9091 grpc.health.v1.Health/Check
grpcurl -plaintext localhost:9092 grpc.health.v1.Health/Check
```

**Performance issues:**
```bash
# Monitor resource usage
make monitor

# Check metrics
curl http://localhost:8080/metrics  # Agents
curl http://localhost:8081/metrics  # Fetcher
```

**AI model issues:**
```bash
# Check model availability
docker exec mosia-agents python -c "import spacy; spacy.load('en_core_web_sm')"
```

### Debug Mode
```bash
# Start services in debug mode
LOG_LEVEL=debug make start

# Access service shells
make shell-agents
make shell-fetcher
```

## 📚 Documentation

- **API Documentation**: Available via Swagger UI in main service
- **gRPC Documentation**: Generated from `.proto` files
- **Architecture Guide**: See `docs/microservices_architecture.md`
- **Development Guide**: See `docs/development_guide.md`

## 🤝 Contributing

1. **Setup development environment**
2. **Create feature branch** 
3. **Implement changes** with tests
4. **Run quality checks**: `make format lint test`
5. **Submit pull request**

### Code Standards
- **Python**: Black formatting, Ruff linting, MyPy type checking
- **Architecture**: Clean architecture with clear layer separation
- **Testing**: Unit tests with >80% coverage
- **Documentation**: Docstrings for all public APIs

---

**🎯 轻量级架构完成 - Week 2 AI功能实现:**
1. ✅ API-based content analysis (OpenAI/Anthropic integration)
2. ✅ Business logic focused recommendations  
3. ✅ Lightweight Docker configuration (512M memory limit)
4. ✅ Removed heavy ML dependencies (transformers, torch, spacy)
5. 🔄 Production-ready microservices with <50% resource usage vs original design