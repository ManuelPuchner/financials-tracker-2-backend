# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven/Gradle wrapper and dependencies first (layer caching)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose default Spring Boot port
EXPOSE 8080

# Environment variables with defaults (override via .env or compose)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]