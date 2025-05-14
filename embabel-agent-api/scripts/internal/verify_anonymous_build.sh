echo "Verifying anonymous build by creating a Docker container without GitHub credentials..."

docker run --rm -v $(pwd)/../..:/project maven:latest bash -c \
  "cd /project && mvn clean install"
