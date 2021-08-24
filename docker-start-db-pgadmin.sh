#!/bin/bash

set -e

printf "\n *** shutdown docker pgadmin instances *** \n"
docker-compose -f docker-compose.yml stop pgadmin

printf "\n *** Starting services *** \n\n"
docker-compose -f docker-compose.yml up -d pgadmin

sleep 5

docker-compose logs
### or to start all services
# docker-compose up
