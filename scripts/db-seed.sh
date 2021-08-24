#!/usr/bin/env bash

export PGPASSWORD=password
psql -h localhost -p 5432 -U postgres -d tenfren -f ./resources/db-schema/tenfren-account-insert.sql
psql -h localhost -p 5432 -U postgres -d tenfren -f ./resources/db-schema/tenfren-role-insert.sql
unset PGPASSWORD
