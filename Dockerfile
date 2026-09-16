# ---------- Stage 1: Build ----------
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom files first for better layer caching
COPY pom.xml .
COPY common/pom.xml common/
COPY monolith/pom.xml monolith/

# Copy source
COPY common/src common/src
COPY monolith/src monolith/src

# Build monolith + common (downloads deps in one pass)
RUN mvn clean install -DskipTests -pl monolith -am

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the executable Spring Boot jar (ignore the .original)
COPY --from=build /app/monolith/target/monolith-*.jar app.jar

# Render sets $PORT; fall back to 9090 if unset
ENV PORT=9090
EXPOSE 9090

# Run with sane memory defaults for a small container
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]