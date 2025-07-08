#!/bin/bash
set -e

echo "Building embedProc..."

# Run tests first
echo "Running tests..."
mvn test

# Build the application
echo "Building application..."
mvn clean package

# Check if Docker is available and running
if command -v docker &> /dev/null && docker info &> /dev/null; then
    echo "Building Docker image..."
docker login
    pack build yourdockerhubuser/embedproc:latest \
  --builder paketobuildpacks/builder-jammy-base \
  --publish \
  --platform linux/amd64,linux/arm64
    echo "Docker image built and published successfully"
else
    echo "Docker not available, skipping container build"
fi

echo "Build completed successfully!"