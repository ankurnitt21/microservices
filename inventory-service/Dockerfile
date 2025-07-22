# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the final image
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=build /app/target/*.jar ./application.jar

# Expose the port for Eureka Discovery Server
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "./application.jar"]