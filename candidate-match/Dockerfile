# Multi-stage build for minimal image size
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven files first for better layer caching
COPY ../pom.xml ./
COPY src ./src
COPY mvnw ./
COPY ../.mvn ./.mvn

# Build the application
RUN ./mvnw clean package -DskipTests

# Final stage - use JRE for smaller image
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -s /bin/sh -u 1000 -G appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/candidate-match.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# Multi-stage build for minimal image size
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven files first for better layer caching
COPY ../pom.xml ./
COPY src ./src
COPY mvnw ./
COPY ../.mvn ./.mvn

# Build the application
RUN ./mvnw clean package -DskipTests

# Final stage - use JRE for smaller image
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -s /bin/sh -u 1000 -G appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/candidate-match.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# Multi-stage build for minimal image size
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven files first for better layer caching
COPY ../pom.xml ./
COPY src ./src
COPY mvnw ./
COPY ../.mvn ./.mvn

# Build the application
RUN ./mvnw clean package -DskipTests

# Final stage - use JRE for smaller image
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -s /bin/sh -u 1000 -G appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/candidate-match.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# Multi-stage build for minimal image size
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven files first for better layer caching
COPY ../pom.xml ./
COPY src ./src
COPY mvnw ./
COPY ../.mvn ./.mvn

# Build the application
RUN ./mvnw clean package -DskipTests

# Final stage - use JRE for smaller image
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -g 1000 appuser && \
    adduser -D -s /bin/sh -u 1000 -G appuser appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/candidate-match.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]