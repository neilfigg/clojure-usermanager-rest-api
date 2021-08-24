#!/bin/bash

set -e

printf "\n *** shutdown docker db instances *** \n"
docker-compose -f docker-compose.yml stop db

printf "\n *** unsetting env varibles *** \n"
source ./scripts/env-unset.sh

printf "\n *** sourcing env *** \n"
source ./scripts/env-dev.sh

printf "\n *** Starting services *** \n\n"
docker-compose -f docker-compose.yml up -d db

sleep 5

docker-compose logs
### or to start all services
# docker-compose up

