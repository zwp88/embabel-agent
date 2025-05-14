echo "Verifying anonymous build by creating a Docker container without GitHub credentials..."

docker run --rm maven:latest bash -c \
  "git clone https://github.com/embabel/embabel-agent && cd /embabel-agent && mvn clean install"
