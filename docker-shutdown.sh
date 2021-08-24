#!/usr/bin/env bash
#cd "$(dirname "$0")"
echo "Shutting down..."
docker-compose -f docker-compose.yml down
