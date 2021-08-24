#!/bin/bash

set -e

printf "\n *** unsetting env varibles *** \n"
source ./scripts/env-unset.sh

printf "\n *** running tests *** \n\n"
clj -M:test:test-kaocha "$@"
