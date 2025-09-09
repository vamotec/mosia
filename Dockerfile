# Mosia Backend API Dockerfile
# 多阶段构建：构建 + 运行时

FROM eclipse-temurin:21-jdk AS builder

# 安装 sbt（注意：这里也要改用适合的包管理器）
RUN apt-get update && \
  apt-get install -y curl && \
  curl -L -o sbt.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-1.11.4.deb && \
  dpkg -i sbt.deb && \
  rm sbt.deb && \
  apt-get clean

WORKDIR /app

# 复制 SBT 配置文件 (利用缓存)
COPY backend/main/project/ ./project/
COPY backend/main/build.sbt ./

# 预下载依赖 (缓存层)
RUN sbt update

# 复制源代码
COPY backend/main/src/ ./src/

# 构建应用
RUN sbt clean compile assembly

# 运行时镜像 - 使用相同的基础系统
FROM eclipse-temurin:21-jre

WORKDIR /app

# 创建非root用户（Debian/Ubuntu 语法）
RUN useradd -r -s /bin/false mosia

# 从构建阶段复制JAR文件
COPY --from=builder /app/target/scala-*/main.jar ./main.jar

# 安装 curl 用于健康检查
RUN apt-get update && apt-get install -y curl && apt-get clean

# 设置权限
RUN chown mosia:mosia main.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:3010/api/health || exit 1

# 暴露端口
EXPOSE 3010 9090

# 切换到非root用户
USER mosia

# 启动应用
CMD ["java", "-Xms512m", "-Xmx1024m", "-jar", "main.jar"]