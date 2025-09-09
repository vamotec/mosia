# Suggested Commands

## Development Commands
```bash
# Start development environment
make dev

# Format code
make format

# Run tests  
make test

# Monitor system
make monitor
```

## Backend Scala Commands
```bash
cd backend/main_service/moscala

# Compile project
sbt compile

# Run application
sbt run

# Run tests
sbt test

# Format code
sbt scalafmtAll

# Clean project
sbt clean
```

## Docker Commands
```bash
# Start all services
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose logs -f [service_name]

# Stop services
docker-compose down
```

## System Utilities (macOS)
```bash
# List files
ls -la

# Search in files (use ripgrep for better performance)
rg "pattern" --type scala

# Find files
find . -name "*.scala" -type f

# Git operations
git status
git add .
git commit -m "message"
git push origin main
```