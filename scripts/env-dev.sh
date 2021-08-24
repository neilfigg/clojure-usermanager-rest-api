#!/bin/bash

# Environment variables

# clojure -X:uberjar :aliases '[:dev]'
# source env-dev.sh
# java -jar tenfren-api.jar
# or
# add socket repl
# java -Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -jar tenfren-api.jar
# nc localhost 5555

# API
export AUTH_JWT_SECRET=somesecretkey
export NOTIFIER_ENABLED=true
export NOTIFIER_HOST=<replace>
export NOTIFIER_USER=<replace>
export NOTIFIER_PASSWORD=<replace>
export NOTIFIER_FROM=neil@tenfren.com

# Postgres
export DB_URL="jdbc:postgresql://tenfren-db/tenfren?user=tenfren&password=password"
export DB_NAME=tenfren
export DB_PORT=5432
export DB_HOST=postgres
export DB_SUPERUSER=postgres
export DB_SUPERUSER_PASSWORD=password
export DB_USER=tenfren
export DB_PASSWORD=password
