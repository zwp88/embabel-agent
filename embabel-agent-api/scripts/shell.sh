#!/usr/bin/env bash

./support/check_env.sh

cd ..
export SPRING_PROFILES_ACTIVE=shell,starwars
mvn spring-boot:run
