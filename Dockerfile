# === Stage 1: Build ===
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:resolve -B

COPY src src
RUN ./mvnw package -DskipTests -B

# === Stage 2: Runtime ===
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S furent && adduser -S furent -G furent

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R furent:furent /app
USER furent

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
