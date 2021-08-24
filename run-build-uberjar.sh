#!/bin/bash

set -e

printf "\n *** unsetting env varibles *** \n"
source ./scripts/env-unset.sh

printf "\n *** running tests *** \n\n"
clj -M:test:test-kaocha "$@"

ALIAS=":dev"

printf "\n *** building jar for ${ALIAS} *** \n\n"

clojure -X:uberjar :aliases [${ALIAS}]

printf "\n *** done! *** \n"

## if api service is not running and the  DB service is running then
## java -Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -jar tenfren-api.jar
