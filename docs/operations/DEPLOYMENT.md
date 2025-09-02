# 部署配置说明

## JAR 部署模式

Scala 后端使用 JAR 文件部署，基础设施使用 Docker。

### 本地开发

```bash
# 启动基础设施（PostgreSQL, Redis, Kafka）
docker-compose -f docker-compose.dev.yml up -d

# 编译并运行 Scala 应用
cd backend/main_service/moscala
sbt run
```

### 打包部署

```bash
# 打包 JAR 文件
./backend/main_service/moscala/scripts/package-jar.sh

# 本地测试 JAR
java -jar backend/main_service/moscala/target/moscala.jar
```

### 服务器部署

#### 手动部署

```bash
# 配置环境变量
export DEPLOY_HOST=your-server.com
export DEPLOY_USER=deploy
export DEPLOY_PATH=/opt/moscala

# 部署
./backend/main_service/moscala/scripts/deploy.sh
```

#### GitHub Actions 自动部署

在 GitHub Repository Settings > Secrets 中配置：

- `DEPLOY_HOST`: 服务器地址
- `DEPLOY_USER`: 部署用户名  
- `DEPLOY_SSH_KEY`: SSH 私钥

推送到 main 分支后自动触发部署。

### 服务器环境要求

```bash
# 创建部署目录
sudo mkdir -p /opt/moscala/logs
sudo chown $USER:$USER /opt/moscala

# 安装 Java 21
sudo apt update
sudo apt install openjdk-21-jre-headless

# 验证 Java 版本
java -version
```

### 环境变量配置

创建 `/opt/moscala/application.conf`：

```hocon
database {
  url = "jdbc:postgresql://localhost:5432/mosia_dev"
  user = "mosia"
  password = "ttr851217"
}

redis {
  host = "localhost"
  port = 6379
}

kafka {
  brokers = ["localhost:9092"]
}

server {
  port = 8080
}
```

⏺ 完全可以一直使用 GitHub Actions！ 建议分阶段演进数据库：

  🎯 推荐策略：起步用 Docker，按需迁移

  阶段 1（现在-1万用户）: Docker PostgreSQL
  - 成本: ~$20/月单服务器
  - 维护: 简单，适合快速迭代
  - 性能: 足够支撑早期业务

  阶段 2（1万-10万用户）: 云数据库服务
  - AWS RDS/Google Cloud SQL
  - 自动备份、监控、扩容
  - 只需修改连接配置，代码无需改动

  📊 迁移时机指标

  继续 Docker 当:
  - 数据量 <50GB
  - 日活 <5000
  - 可接受偶尔重启

  迁移到云服务当:
  - 数据量 >100GB
  - 需要 99.9% 可用性
  - 多地域部署需求

  💡 最佳实践

  现在就做好准备:
  - 抽象数据库配置（已在你的 application.conf 中）
  - 定期数据备份脚本
  - 监控数据库性能指标

  这种渐进式架构很明智 - 控制早期成本，保持扩展性！