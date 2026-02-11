# -----------------------------------------------------------------------------
# Stage 1: Build the Application (Gradle)
# -----------------------------------------------------------------------------
FROM gradle:8.14.3-jdk21-alpine AS builder

WORKDIR /app

# 1. Copy dependency definitions first (to cache dependencies)
COPY build.gradle.kts settings.gradle.kts ./

# 2. Download dependencies (This layer will be cached unless gradle files change)
#    Note: We use --no-daemon to save memory in CI/Docker environments
RUN gradle dependencies --no-daemon

# 3. Copy the actual source code
COPY src ./src

# 4. Build the JAR (Skip tests to speed up build, assuming CI ran them)
RUN gradle bootJar --no-daemon -x test

# -----------------------------------------------------------------------------
# Stage 2: Run the Application (JRE)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 1. Create a non-root user for security (Best Practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 2. Copy the built JAR from the builder stage
#    We use a wildcard *.jar because the version number might change
COPY --from=builder /app/build/libs/*.jar app.jar

# 3. Expose the port (Documentation only)
EXPOSE 8081

# 4. Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]