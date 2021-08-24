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
unset AUTH_JWT_SECRET
unset SERVER_PORT

# Notifier
unset NOTIFIER_ENABLED
unset NOTIFIER_HOST
unset NOTIFIER_USER
unset NOTIFIER_PASSWORD
unset NOTIFIER_FROM

# Postgres
unset DB_URL
unset DB_NAME
unset DB_PORT
unset DB_HOST
unset DB_SUPERUSER
unset DB_SUPERUSER_PASSWORD
unset DB_USER
unset DB_PASSWORD
