# Code Style and Conventions

## Scala Conventions
- **Naming**: CamelCase for classes, camelCase for methods and variables
- **Indentation**: 2 spaces
- **Line Length**: 120 characters max
- **Formatting**: Use scalafmt with provided .scalafmt.conf
- **Architecture**: Clean Architecture with layers:
  - `interface/` - HTTP endpoints, controllers, middleware
  - `application/` - DTOs and application services
  - `domain/` - Business models and logic
  - `infra/` - Infrastructure implementations (repos, cache, etc.)
  - `core/` - Configuration, errors, types

## Python Conventions (for microservices)
- **Naming**: snake_case for functions and variables, PascalCase for classes
- **Type Hints**: Always use type hints
- **Dependencies**: Use Poetry with pyproject.toml
- **Architecture**: Modular design with clear separation of concerns

## Project Structure
```
backend/
├── main_service/moscala/     # Scala ZIO application
└── micro_service/           # Python microservices
    ├── agents/             # AI agent services
    └── fetcher/           # Data fetching services
```

## Commit Messages
- Use conventional commits format
- Examples: "feat:", "fix:", "docs:", "refactor:"