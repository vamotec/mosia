# Multi-stage build for Scala application optimization
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.2_13_1.9.9_3.4.0 AS builder

# Set working directory
WORKDIR /app

# Copy build files for dependency caching
COPY project/ project/
COPY build.sbt .

# Download dependencies (cached layer)
RUN sbt update

# Copy source code
COPY src/ src/

# Build the application
RUN sbt assembly

# Production stage with minimal JRE
FROM eclipse-temurin:21-jre-jammy

# Create non-root user for security
RUN groupadd -r mosia && useradd -r -g mosia mosia

# Set working directory
WORKDIR /app

# Install required packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy built JAR from builder stage
COPY --from=builder /app/target/scala-*/moscala-assembly-*.jar app.jar

# Change ownership to non-root user
RUN chown -R mosia:mosia /app

# Switch to non-root user
USER mosia

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:3010/health || exit 1

# Expose port
EXPOSE 3010 9090

# Set JVM options for production
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -Xmx2g -Xms1g"

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]