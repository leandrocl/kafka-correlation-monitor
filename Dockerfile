# Stage 1: Build the application using Maven and Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the Project Object Model (pom.xml) and download dependencies
# This is done as a separate step to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Package the application, skipping tests
RUN mvn package -DskipTests

# Stage 2: Create the final runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR file from the build stage
# The JAR filename might need to be adjusted based on your pom.xml <artifactId> and <version>
COPY --from=build /app/target/*.jar app.jar

# Expose the port your application will run on (e.g., 8080 for a web app)
# EXPOSE 8080

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]