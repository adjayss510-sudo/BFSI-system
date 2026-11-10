# syntax=docker/dockerfile:1.4
# One Dockerfile for every Spring Boot service. Build with:  --build-arg SERVICE=<module-name>
# Build context must be the repository root (the parent pom lives there).

# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -pl ${SERVICE} -am clean package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre
ARG SERVICE
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /build/${SERVICE}/target/${SERVICE}-1.0.0.jar app.jar
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-jar", "app.jar"]
