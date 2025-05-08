mvn clean package -DskipTests
docker login
pack build yourdockerhubuser/textproc:latest \
  --builder paketobuildpacks/builder-jammy-base \
  --publish \
  --platform linux/amd64,linux/arm64