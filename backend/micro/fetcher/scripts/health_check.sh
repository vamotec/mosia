#!/bin/bash
# Health check script for Fetcher microservice

set -euo pipefail

# Configuration
GRPC_PORT=${GRPC_PORT:-50052}
HOST=${HOST:-localhost}
TIMEOUT=${HEALTH_TIMEOUT:-10}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check gRPC health
check_grpc_health() {
    echo "Checking gRPC health on ${HOST}:${GRPC_PORT}..."
    
    # Use grpcurl if available, otherwise fall back to python
    if command -v grpcurl &> /dev/null; then
        if grpcurl -plaintext -max-time ${TIMEOUT} ${HOST}:${GRPC_PORT} grpc.health.v1.Health/Check; then
            echo -e "${GREEN}✓ gRPC service is healthy${NC}"
            return 0
        else
            echo -e "${RED}✗ gRPC service is unhealthy${NC}"
            return 1
        fi
    else
        # Fallback to python check
        python3 -c "
import grpc
import sys
from grpc_health.v1 import health_pb2, health_pb2_grpc

try:
    channel = grpc.insecure_channel('${HOST}:${GRPC_PORT}')
    stub = health_pb2_grpc.HealthStub(channel)
    request = health_pb2.HealthCheckRequest()
    response = stub.Check(request, timeout=${TIMEOUT})
    if response.status == health_pb2.HealthCheckResponse.SERVING:
        print('gRPC service is healthy')
        sys.exit(0)
    else:
        print('gRPC service is unhealthy')
        sys.exit(1)
except Exception as e:
    print(f'Health check failed: {e}')
    sys.exit(1)
        "
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ gRPC service is healthy${NC}"
            return 0
        else
            echo -e "${RED}✗ gRPC service is unhealthy${NC}"
            return 1
        fi
    fi
}

# Function to check dependencies
check_dependencies() {
    echo "Checking service dependencies..."
    
    # Check if required Python packages are available
    python3 -c "
import sys
required_packages = [
    'grpc', 'pydantic', 'structlog',
    'aiohttp', 'pandas'
]

missing_packages = []
for package in required_packages:
    try:
        __import__(package)
    except ImportError:
        missing_packages.append(package)

if missing_packages:
    print(f'Missing packages: {missing_packages}')
    sys.exit(1)
else:
    print('All required packages are available')
    sys.exit(0)
    "
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ All dependencies are available${NC}"
        return 0
    else
        echo -e "${RED}✗ Some dependencies are missing${NC}"
        return 1
    fi
}

# Function to check Chrome (for Selenium)
check_chrome() {
    echo "Checking Chrome availability..."
    
    if command -v google-chrome &> /dev/null; then
        echo -e "${GREEN}✓ Chrome is available${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ Chrome not found (web scraping may be limited)${NC}"
        return 0  # Non-critical
    fi
}

# Function to check storage
check_storage() {
    echo "Checking storage configuration..."
    
    TEMP_PATH=${TEMP_STORAGE_PATH:-/tmp/mosia_fetcher}
    
    if [ ! -d "$TEMP_PATH" ]; then
        echo "Creating temporary storage directory: $TEMP_PATH"
        mkdir -p "$TEMP_PATH" 2>/dev/null || true
    fi
    
    if [ -w "$TEMP_PATH" ]; then
        echo -e "${GREEN}✓ Storage is writable${NC}"
        return 0
    else
        echo -e "${RED}✗ Storage is not writable${NC}"
        return 1
    fi
}

# Main health check
main() {
    echo "=== Fetcher Service Health Check ==="
    echo "Timestamp: $(date)"
    echo "Host: ${HOST}:${GRPC_PORT}"
    echo

    local exit_code=0

    # Check dependencies
    if ! check_dependencies; then
        exit_code=1
    fi
    echo

    # Check Chrome
    check_chrome
    echo

    # Check storage
    if ! check_storage; then
        exit_code=1
    fi
    echo

    # Check gRPC health
    if ! check_grpc_health; then
        exit_code=1
    fi
    echo

    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}=== All health checks passed ✓ ===${NC}"
    else
        echo -e "${RED}=== Health check failed ✗ ===${NC}"
    fi

    exit $exit_code
}

# Run health check
main "$@"