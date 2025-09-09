# Task Completion Workflow

## When a task is completed:

### 1. Code Quality Checks
```bash
# For Scala code
cd backend/main_service/moscala
sbt scalafmtCheck
sbt compile
sbt test

# For Python code  
cd backend/micro_service/[service_name]
poetry run black .
poetry run mypy .
poetry run pytest
```

### 2. Docker Validation
```bash
# Build and test containers
docker build -t service-name .
docker run --rm service-name
```

### 3. Integration Testing
```bash
# Start development environment
make dev
# Verify all services are healthy
docker-compose ps
```

### 4. Documentation Update
- Update README if needed
- Update API documentation
- Add/update comments for complex logic

### 5. Git Workflow
```bash
git add .
git commit -m "feat: implement [feature description]"
# Only push after all validations pass
git push origin feature-branch
```