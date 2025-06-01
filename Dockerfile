FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests
# ---- Run the app ----
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

RUN apt-get update && apt-get install -y curl net-tools && rm -rf /var/lib/apt/lists/*


WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Use ENTRYPOINT with port logging and netstat for debugging
ENTRYPOINT ["/bin/bash", "-c", "\
  echo 'Starting Spring Boot application...'; \
  java -jar app.jar & \
  APP_PID=$!; \
  echo 'Spring Boot PID: '$APP_PID; \
  sleep 10; \
  echo '--- Internal Netstat Check ---'; \
  netstat -tuln; \
  echo '--- Internal Lsof Check ---'; \
  lsof -i -P -n || echo 'lsof not found or failed'; \
  echo '--- Internal Curl Health Check ---'; \
  curl -v http://0.0.0.0:8080/actuator/health/liveness || true; \
  echo '--- End Internal Checks ---'; \
  wait $APP_PID \
"]