# 🔍 Mosia监控快速启动指南

## 🎯 推荐方案：渐进式监控

### 第一阶段: 基础监控 (立即开始)

```bash
# 1. 启动简化监控栈
cd devops/monitoring
docker-compose -f docker-compose.simple.yml up -d

# 2. 验证服务状态
docker-compose ps

# 3. 访问监控界面
open http://localhost:9090  # Prometheus
```

**获得什么**:
- ✅ 系统资源监控 (CPU/内存/磁盘)
- ✅ 数据库性能指标
- ✅ API健康状态检查
- ✅ 简单的Prometheus查询界面

### 第二阶段: 可视化监控 (需要时添加)

```bash
# 当你想要图表时，升级到完整栈
docker-compose -f docker-compose.simple.yml down
docker-compose -f docker-compose.monitoring.yml up -d

# 访问Grafana
open http://localhost:3000  # 用户: admin, 密码: admin
```

---

## 📊 关键监控指标说明

### 🎯 你需要关注的核心指标

#### API服务健康
```promql
# API响应时间
http_request_duration_seconds

# API请求数量
http_requests_total

# API错误率
rate(http_requests_total{status=~"5.."}[5m])
```

#### 数据库性能
```promql
# 数据库连接数
pg_stat_database_numbackends

# 慢查询数量  
pg_stat_database_tup_returned

# 数据库大小
pg_database_size_bytes
```

#### 系统资源
```promql
# CPU使用率
100 - (avg(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 内存使用率
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 磁盘使用率
100 - ((node_filesystem_avail_bytes / node_filesystem_size_bytes) * 100)
```

---

## 🚨 告警设置建议

### 基础告警规则

**严重告警** (立即处理):
```yaml
# API服务下线
- alert: MosiaAPIDown
  expr: up{job="mosia-backend-api"} == 0
  for: 1m

# 数据库连接失败
- alert: PostgreSQLDown  
  expr: up{job="postgres"} == 0
  for: 1m
```

**警告告警** (关注但不紧急):
```yaml
# 高CPU使用
- alert: HighCPUUsage
  expr: cpu_usage > 80
  for: 5m

# 磁盘空间不足
- alert: DiskSpaceLow
  expr: disk_usage > 85
  for: 10m
```

---

## 🛠️ 实际操作步骤

### 立即可执行 (今天就能完成)

#### 1. 测试简化监控
```bash
# 启动基础监控
cd devops/monitoring
docker-compose -f docker-compose.simple.yml up -d

# 检查状态
docker-compose ps

# 测试Prometheus
curl http://localhost:9090/api/v1/query?query=up
```

#### 2. 验证数据收集
```bash
# 访问Prometheus Web UI
open http://localhost:9090

# 在界面中查询：
up                    # 查看所有服务状态
node_cpu_seconds_total  # 查看CPU指标
pg_up                 # 查看数据库状态
```

#### 3. 基础告警测试
```bash
# 模拟API故障 (测试告警)
docker stop mosia-app

# 1分钟后检查Prometheus Alert页面
# 应该看到 MosiaAPIDown 告警

# 恢复服务
docker start mosia-app
```

---

## 📈 监控成熟度路径

### Level 1: 基础监控 (当前推荐)
**目标**: 知道系统是否正常运行
```
✅ 服务健康检查
✅ 基础系统指标
✅ 数据库连接状态
❌ 无可视化界面
❌ 无自动告警通知
```

### Level 2: 可视化监控 (2周后)
**目标**: 看到趋势和图表
```
✅ Grafana仪表板
✅ 历史趋势分析
✅ 自定义图表
❌ 无自动通知
```

### Level 3: 智能告警 (1个月后)
**目标**: 问题自动通知
```
✅ 邮件/Slack通知
✅ 告警规则优化
✅ 故障自动恢复
```

---

## 💡 推荐实施策略

### 🥇 最优方案：渐进式实施

**本周**: 启动简化监控，习惯Prometheus查询
**下周**: 添加Grafana，创建基础仪表板  
**下个月**: 配置告警通知，完善监控规则

### 🎯 立即行动建议

```bash
# 今天就可以执行：
1. cd devops/monitoring
2. docker-compose -f docker-compose.simple.yml up -d
3. 访问 http://localhost:9090
4. 查询 "up" 指标验证工作正常
5. 每天检查一次，熟悉界面
```

**优势**: 
- 学习成本低
- 立即获得价值
- 随时可升级到完整栈

---

## ⚠️ 避免过度复杂化

**不要一开始就**：
- 配置复杂的告警规则
- 创建过多的仪表板
- 收集过量的指标
- 设置Slack/邮件通知

**先专注于**：
- 基础健康监控
- 关键性能指标
- 简单的查询和分析

<function_calls>
<invoke name="TodoWrite">
<parameter name="todos">[{"content": "\u5220\u9664k8s\u76f8\u5173\u6587\u4ef6\u5939\u51cf\u5c11\u590d\u6742\u5ea6", "status": "completed", "activeForm": "\u5220\u9664k8s\u76f8\u5173\u6587\u4ef6\u5939\u51cf\u5c11\u590d\u6742\u5ea6"}, {"content": "\u5206\u6790\u5f53\u524d\u76d1\u63a7\u914d\u7f6e\u72b6\u6001", "status": "completed", "activeForm": "\u5206\u6790\u5f53\u524d\u76d1\u63a7\u914d\u7f6e\u72b6\u6001"}, {"content": "\u8bbe\u8ba1\u7b80\u5316\u7684\u76d1\u63a7\u65b9\u6848", "status": "completed", "activeForm": "\u8bbe\u8ba1\u7b80\u5316\u7684\u76d1\u63a7\u65b9\u6848"}, {"content": "\u63d0\u4f9bmonitoring\u5b9e\u65bd\u5efa\u8bae", "status": "completed", "activeForm": "\u63d0\u4f9bmonitoring\u5b9e\u65bd\u5efa\u8bae"}]