#!/bin/bash

# ==============================================================================
# Production Deployment Script for Fetcher Microservice
# Handles Docker build, deployment, and health checks with Poetry
# ==============================================================================

set -euo pipefail

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SERVICE_NAME="fetcher"
IMAGE_NAME="mosia/fetcher"
COMPOSE_FILE="docker-compose.poetry.yml"
ENV_FILE=".env"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Print usage
usage() {
    cat << EOF
Usage: $0 [COMMAND] [OPTIONS]

Commands:
    build           Build the Docker image
    deploy          Deploy the service stack
    start           Start the service stack
    stop            Stop the service stack
    restart         Restart the service stack
    status          Show service status
    logs            Show service logs
    health          Check service health
    cleanup         Clean up unused resources
    test            Run tests in container
    dev             Start development environment

Options:
    --version VERSION   Specify image version (default: latest)
    --env ENV_FILE     Specify environment file (default: .env)
    --no-cache         Build without cache
    --profile PROFILE  Use specific compose profile (dev, prod)
    --help             Show this help message

Examples:
    $0 build --version v1.2.3
    $0 deploy --env .env.production
    $0 dev
    $0 logs fetcher
EOF
}

# Parse command line arguments
COMMAND=""
VERSION="latest"
NO_CACHE=""
PROFILE="prod"

while [[ $# -gt 0 ]]; do
    case $1 in
        build|deploy|start|stop|restart|status|logs|health|cleanup|test|dev)
            COMMAND="$1"
            shift
            ;;
        --version)
            VERSION="$2"
            shift 2
            ;;
        --env)
            ENV_FILE="$2"
            shift 2
            ;;
        --no-cache)
            NO_CACHE="--no-cache"
            shift
            ;;
        --profile)
            PROFILE="$2"
            shift 2
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Ensure we're in the project directory
cd "$PROJECT_DIR"

# Check dependencies
check_dependencies() {
    log_info "Checking dependencies..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi
    
    if [[ ! -f "$ENV_FILE" ]]; then
        log_warning "Environment file $ENV_FILE not found"
        if [[ -f ".env.docker" ]]; then
            log_info "Copying .env.docker to $ENV_FILE"
            cp .env.docker "$ENV_FILE"
        else
            log_error "No environment file template found"
            exit 1
        fi
    fi
    
    log_success "Dependencies check passed"
}

# Build the Docker image
build_image() {
    log_info "Building Docker image: $IMAGE_NAME:$VERSION"
    
    export VERSION="$VERSION"
    
    docker-compose -f "$COMPOSE_FILE" build $NO_CACHE fetcher
    
    # Tag the image
    docker tag "mosia/fetcher:latest" "$IMAGE_NAME:$VERSION"
    
    log_success "Image built successfully: $IMAGE_NAME:$VERSION"
}

# Deploy the service stack
deploy_service() {
    log_info "Deploying $SERVICE_NAME service stack..."
    
    export VERSION="$VERSION"
    
    # Create necessary directories
    mkdir -p data logs config/monitoring
    
    # Pull latest images for dependencies
    docker-compose -f "$COMPOSE_FILE" pull postgres redis
    
    # Deploy the stack
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --remove-orphans
    
    # Wait for services to be healthy
    wait_for_health
    
    log_success "Service stack deployed successfully"
}

# Start services
start_services() {
    log_info "Starting $SERVICE_NAME services..."
    
    export VERSION="$VERSION"
    
    if [[ "$PROFILE" == "dev" ]]; then
        docker-compose -f "$COMPOSE_FILE" --profile dev --env-file "$ENV_FILE" up -d
    else
        docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
    fi
    
    log_success "Services started"
}

# Stop services
stop_services() {
    log_info "Stopping $SERVICE_NAME services..."
    
    docker-compose -f "$COMPOSE_FILE" down
    
    log_success "Services stopped"
}

# Restart services
restart_services() {
    log_info "Restarting $SERVICE_NAME services..."
    
    stop_services
    sleep 2
    start_services
    
    log_success "Services restarted"
}

# Show service status
show_status() {
    log_info "Service status:"
    
    docker-compose -f "$COMPOSE_FILE" ps
    
    echo
    log_info "Resource usage:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"
}

# Show service logs
show_logs() {
    local service="${1:-fetcher}"
    
    log_info "Showing logs for service: $service"
    
    docker-compose -f "$COMPOSE_FILE" logs -f --tail=100 "$service"
}

# Health check
check_health() {
    log_info "Checking service health..."
    
    # Check if containers are running
    local containers=$(docker-compose -f "$COMPOSE_FILE" ps -q)
    
    if [[ -z "$containers" ]]; then
        log_error "No containers are running"
        return 1
    fi
    
    # Check specific service health
    local fetcher_health=$(docker-compose -f "$COMPOSE_FILE" ps fetcher | grep "healthy" | wc -l)
    
    if [[ "$fetcher_health" -eq 1 ]]; then
        log_success "Fetcher service is healthy"
    else
        log_warning "Fetcher service health check failed"
        docker-compose -f "$COMPOSE_FILE" logs --tail=20 fetcher
    fi
    
    # Test gRPC endpoint
    local grpc_port=$(grep GRPC_PORT "$ENV_FILE" | cut -d= -f2)
    grpc_port=${grpc_port:-50052}
    
    if command -v grpcurl &> /dev/null; then
        log_info "Testing gRPC endpoint..."
        if grpcurl -plaintext localhost:$grpc_port list > /dev/null 2>&1; then
            log_success "gRPC endpoint is responding"
        else
            log_warning "gRPC endpoint is not responding"
        fi
    fi
}

# Wait for services to be healthy
wait_for_health() {
    log_info "Waiting for services to be healthy..."
    
    local max_attempts=60
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        local healthy_services=$(docker-compose -f "$COMPOSE_FILE" ps | grep "healthy" | wc -l)
        local total_services=$(docker-compose -f "$COMPOSE_FILE" ps | grep -v "Exit 0" | tail -n +2 | wc -l)
        
        log_info "Health check attempt $attempt/$max_attempts: $healthy_services/$total_services services healthy"
        
        if [[ "$healthy_services" -eq "$total_services" ]] && [[ "$total_services" -gt 0 ]]; then
            log_success "All services are healthy"
            return 0
        fi
        
        sleep 10
        ((attempt++))
    done
    
    log_warning "Timeout waiting for services to be healthy"
    show_status
    return 1
}

# Cleanup unused resources
cleanup_resources() {
    log_info "Cleaning up unused Docker resources..."
    
    # Stop services
    docker-compose -f "$COMPOSE_FILE" down --remove-orphans
    
    # Clean up unused images, containers, networks
    docker system prune -f
    
    # Clean up unused volumes (be careful!)
    read -p "Do you want to clean up unused volumes? This will delete data! [y/N]: " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker volume prune -f
        log_warning "Unused volumes have been removed"
    fi
    
    log_success "Cleanup completed"
}

# Run tests
run_tests() {
    log_info "Running tests in container..."
    
    # Build test image
    docker-compose -f "$COMPOSE_FILE" build --target test fetcher
    
    # Run tests
    docker-compose -f "$COMPOSE_FILE" run --rm fetcher poetry run pytest tests/ -v
    
    log_success "Tests completed"
}

# Start development environment
start_dev() {
    log_info "Starting development environment..."
    
    export VERSION="dev"
    PROFILE="dev"
    
    # Start with development profile
    docker-compose -f "$COMPOSE_FILE" --profile dev --env-file "$ENV_FILE" up -d
    
    log_success "Development environment started"
    log_info "Fetcher service available at: localhost:50053"
    log_info "Grafana dashboard: http://localhost:3000"
    log_info "Use 'docker-compose -f $COMPOSE_FILE logs -f fetcher-dev' to see logs"
}

# Main execution
main() {
    if [[ -z "$COMMAND" ]]; then
        log_error "No command specified"
        usage
        exit 1
    fi
    
    check_dependencies
    
    case "$COMMAND" in
        build)
            build_image
            ;;
        deploy)
            build_image
            deploy_service
            ;;
        start)
            start_services
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "${2:-fetcher}"
            ;;
        health)
            check_health
            ;;
        cleanup)
            cleanup_resources
            ;;
        test)
            run_tests
            ;;
        dev)
            start_dev
            ;;
        *)
            log_error "Unknown command: $COMMAND"
            usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"