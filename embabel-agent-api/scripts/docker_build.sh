cd ..

echo "Building and pushing Docker image for embabel-agent-api..."

mvn -DskipTests spring-boot:build-image
docker tag embabel-agent-api:1.0.0-SNAPSHOT springrod/embabel-agent-api:1.0.0-SNAPSHOT

docker push springrod/embabel-agent-api:1.0.0-SNAPSHOT