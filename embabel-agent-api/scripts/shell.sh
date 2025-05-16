#!/usr/bin/env bash

./support/check_env.sh

cd ..
export SPRING_PROFILES_ACTIVE=shell,starwars,docker-desktop
mvn spring-boot:run
