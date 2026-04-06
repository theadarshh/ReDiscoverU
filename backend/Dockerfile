# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
# Download dependencies first (layer caching — re-runs only if pom.xml changes)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Run ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create uploads directory — persisted via volume mount on Render
RUN mkdir -p /app/uploads/mentors /app/uploads/motivation /app/uploads/static

COPY --from=build /app/target/rediscoveru-*.jar app.jar

# Render sets PORT automatically — default 8080
EXPOSE 8080

# Run with production-friendly JVM flags
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
