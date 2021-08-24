#!/bin/bash

set -e

printf "\n *** shutdown docker instances *** \n"
./run-docker-shutdown.sh

printf "\n *** unsetting env varibles *** \n"
source ./scripts/env-unset.sh

printf "\n *** running tests *** \n\n"
clj -M:test:test-kaocha "$@"

printf "\n *** compiling jar *** \n\n"
clojure -X:uberjar :aliases '[:dev]'

printf "\n *** sourcing env *** \n"
source ./scripts/env-dev.sh

printf "\n *** Starting services *** \n\n"
docker-compose -f docker-compose.yml up --build -d

sleep 5

docker-compose logs
### or to start individual services
# docker-compose up -d db
# docker-compose up -d api
