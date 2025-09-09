# Mosia Project Overview

## Purpose
Mosia is an AI-Enhanced Collaborative Workspace platform that combines AI capabilities with team collaboration tools. It provides intelligent project management, real-time communication, document management, and data analytics.

## Tech Stack
### Backend
- **Main Service**: Scala 3.3.6 with ZIO + ZIO-HTTP framework
- **Microservices**: Python (AI agents and data fetcher)
- **API**: Tapir + Swagger documentation
- **Database**: PostgreSQL 15+ with Quill ORM
- **Cache**: Redis 7+
- **Message Queue**: Apache Kafka
- **Authentication**: JWT + OAuth2

### Frontend
- **Framework**: Flutter 3.24.0+ with Dart 3.0+
- **State Management**: Riverpod
- **Network**: Dio + Retrofit

### DevOps
- **Containerization**: Docker + Docker Compose
- **Orchestration**: Kubernetes 1.28+
- **CI/CD**: GitHub Actions
- **Monitoring**: Prometheus + Grafana
- **Infrastructure**: Terraform + AWS

## Architecture
```
Frontend (Flutter) ←→ Scala API ←→ Python AI Microservices
                           ↓
                   PostgreSQL + Redis + Kafka
```

## Current Status
- ✅ Week 1: Backend framework and DevOps completed
- 🔄 Week 2: AI Engineer tasks (current focus)