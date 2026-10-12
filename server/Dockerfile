# Multi-stage Dockerfile for SecureChat Spring Boot backend
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src src
RUN mvn package -DskipTests -B

# Stage 2: Runtime image with non-root user
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built artifact
COPY --from=builder /workspace/target/securechat-server-*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8090

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
