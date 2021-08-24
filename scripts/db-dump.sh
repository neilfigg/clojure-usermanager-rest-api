#!/usr/bin/env bash

export PGPASSWORD=password
pg_dump -h localhost -p 5432 -U postgres -d tenfren --schema-only -f ./resources/db-schema/tenfren-dump.ddl
pg_dump -h localhost -p 5432 -U postgres --table=account --data-only --column-inserts tenfren -f ./resources/db-schema/tenfren-account-insert.sql
pg_dump -h localhost -p 5432 -U postgres --table=role --data-only --column-inserts tenfren -f ./resources/db-schema/tenfren-role-insert.sql
unset PGPASSWORD
