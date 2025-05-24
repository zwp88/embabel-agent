@echo off
setlocal

call .\support\check_env.bat

if errorlevel 1 (
    echo Environment check failed. Exiting...
    exit /b 1
)

cd ..

set SPRING_PROFILES_ACTIVE=shell,severance,observability

cmd /c mvn spring-boot:run

endlocal