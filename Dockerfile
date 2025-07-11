FROM openjdk:17-jdk-slim-bullseye

WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven

# Copy pom.xml first for better caching
COPY pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Run the application
EXPOSE 8080
CMD ["java", "-jar", "target/candidate-match.jar"]