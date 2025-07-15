# Steg 1: Bygg applikasjonen med Maven
FROM maven:3.8.5-openjdk-17 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

# Steg 2: Bygg det endelige, lettvekts imaget
FROM openjdk:17-jdk-slim
COPY --from=build /home/app/target/candidate-match.jar /usr/local/lib/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/app.jar"]