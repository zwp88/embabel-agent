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

pushd .
cd ..
set SPRING_PROFILES_ACTIVE=shell,severance
start cmd /k mvn spring-boot:run
set SPRING_PROFILES_ACTIVE=
popd