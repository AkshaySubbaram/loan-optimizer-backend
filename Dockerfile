# Use Eclipse Temurin JDK 17 as the base image
FROM eclipse-temurin:17-jdk-alpine

# Set a build argument for the JAR file (from Maven target)
ARG JAR_FILE=target/*.jar

# Copy the Spring Boot JAR into the container
COPY ${JAR_FILE} app.jar

# Expose the port your Spring Boot app runs on
EXPOSE 8080

# Command to run the Spring Boot app
ENTRYPOINT ["java", "-jar", "/app.jar"]