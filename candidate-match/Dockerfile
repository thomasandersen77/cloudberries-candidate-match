# syntax=docker/dockerfile:1

# Build stage: use JDK 21 and install Maven
FROM eclipse-temurin:21-jdk-jammy@sha256:adb9b2d15adf1833d9dae0bdc1cff61ef5a804dc58dfbfb34269f32432b2e5dc AS build
WORKDIR /workspace

# Install Maven (Debian/Ubuntu)
RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/*

# Copy only POMs first to leverage layer caching
COPY pom.xml ./
COPY candidate-match/pom.xml candidate-match/pom.xml
# Ensure parent aggregator's child module directories exist so Maven doesn't fail
COPY ai-rag-service/pom.xml ai-rag-service/pom.xml
COPY teknologi-barometer-service/pom.xml teknologi-barometer-service/pom.xml

# Pre-fetch dependencies for the candidate-match module
RUN mvn -B -DskipTests -DskipITs=true -pl candidate-match -am dependency:go-offline

# Copy source for the candidate-match module
COPY candidate-match/src candidate-match/src

# Build only the candidate-match module
RUN mvn -B -DskipTests -DskipITs=true -pl candidate-match -am clean package

# Runtime stage: JRE 21 with curl for healthcheck
FROM eclipse-temurin:21-jre-jammy@sha256:db1689535962d757a5adabf57387584ed543d38c0b9d1fe870123ea362ad73b0 AS runtime

# Install curl for HEALTHCHECK
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd --system appuser && useradd --system --no-create-home --gid appuser --shell /usr/sbin/nologin appuser

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /workspace/candidate-match/target/candidate-match.jar app.jar

# Set ownership to non-root user
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check using curl against Spring Boot actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health >/dev/null || exit 1

# Bind to 0.0.0.0 and honor PORT if provided by the platform
# Use container profile for minimal startup without database dependencies
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-8080} -Dspring.profiles.active=container -jar app.jar"]
