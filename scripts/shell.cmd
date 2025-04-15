@echo off
REM Check if OPENAI_API_KEY is set
if "%OPENAI_API_KEY%"=="" (
    echo Error: OPENAI_API_KEY environment variable is not set
    echo OpenAI will not be available
    echo Please set it with: set OPENAI_API_KEY=your_api_key
    exit /b 1
) else (
    echo OPENAI_API_KEY set: OpenAI models are available
)

REM Check if BRAVE_API_KEY is set
if "%BRAVE_API_KEY%"=="" (
    echo Warning: BRAVE_API_KEY environment variable is not set.
    echo Search features will not work properly without it.
    echo You can get an API key at https://brave.com/search/api/
    echo You can set it with: set BRAVE_API_KEY=your_api_key
) else (
    echo BRAVE_API_KEY set: Web, news and video search available
)

pushd .
cd ..
set SPRING_PROFILES_ACTIVE=shell,severance
start cmd /c mvn spring-boot:run
set SPRING_PROFILES_ACTIVE=
popd