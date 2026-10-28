FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --home-dir /app saleslens

WORKDIR /app
COPY --from=build --chown=saleslens:saleslens /workspace/target/sales-lens-0.0.1-SNAPSHOT.jar app.jar
USER saleslens

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
