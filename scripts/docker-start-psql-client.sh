#!/usr/bin/env bash
## make sure the db container is up
docker exec -it tenfren-db psql -U postgres -d tenfren
