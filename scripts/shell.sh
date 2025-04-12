#!/usr/bin/env bash

if [ -z "${OPENAI_API_KEY}" ]; then
    echo "Error: OPENAI_API_KEY environment variable is not set"
    echo "OpenAI will not be available"
    echo "Please set it with: export OPENAI_API_KEY=your_api_key"
    exit 1
else
    echo "OPENAI_API_KEY set: OpenAI models are available"
fi

if [ -z "${BRAVE_API_KEY}" ]; then
    echo "Warning: BRAVE_API_KEY environment variable is not set."
    echo "Search features will not work properly without it."
    echo "You can get an API key at https://brave.com/search/api/"
    echo "You can set it with: export BRAVE_API_KEY=your_api_key"
else
    echo "BRAVE_API_KEY set: Web, news and video search available"
fi

cd ..
export SPRING_PROFILES_ACTIVE=shell,severance
mvn spring-boot:run
