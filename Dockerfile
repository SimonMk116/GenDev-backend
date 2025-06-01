    FROM maven:3.9.4-eclipse-temurin-21 AS build
    WORKDIR /app
    COPY . .
    RUN mvn clean package -DskipTests
    # ---- Run the app ----
    FROM eclipse-temurin:21-jdk
    # Install debugging tools (e.g., netstat, lsof)
    RUN apt-get update && \
        apt-get install -y net-tools lsof curl && \
        apt-get clean

    WORKDIR /app
    COPY --from=build /app/target/*.jar app.jar
    # Use ENTRYPOINT with port logging and netstat for debugging
    ENTRYPOINT ["sh", "-c", "\
        echo 'Listening on port: ${PORT:-8080}' && \
        java -jar app.jar --server.port=${PORT:-8080} & \
        sleep 5 && \
        echo '--- Netstat output ---' && netstat -tuln && \
        echo '--- Lsof output ---' && lsof -i -P -n && \
        wait \
    "]
