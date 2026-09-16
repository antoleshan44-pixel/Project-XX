# Stage 1: Build
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY monolith/pom.xml monolith/

COPY common/src common/src
COPY monolith/src monolith/src

RUN mvn clean install -DskipTests -pl monolith -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/monolith/target/monolith-*.jar app.jar

ENV PORT=9090
EXPOSE 9090

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
