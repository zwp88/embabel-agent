# Check if OPENAI_API_KEY is set
if (-not $env:OPENAI_API_KEY) {
    Write-Host "Error: OPENAI_API_KEY environment variable is not set" -ForegroundColor Red
    Write-Host "OpenAI will not be available"
    Write-Host "Please set it with: `$env:OPENAI_API_KEY = 'your_api_key'"
    exit 1
} else {
    Write-Host "OPENAI_API_KEY set: OpenAI models are available" -ForegroundColor Green
}


# Navigate up one directory
Set-Location ..

# Set Spring profile and run Maven
$env:SPRING_PROFILES_ACTIVE = "shell,severance"
mvn spring-boot:run