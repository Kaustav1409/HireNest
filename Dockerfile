# Stage 1: Build the application using Maven and JDK 17
FROM maven:3.8.8-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy pom.xml and the libs directory containing the base JAR
COPY pom.xml .
COPY libs ./libs

# Copy the source code
COPY src ./src

# Build the Spring Boot application (skipping tests for faster cloud deployment)
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight runtime image using JRE 17
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the generated JAR from the builder stage
COPY --from=builder /build/target/hirenest-backend-0.0.1-SNAPSHOT.jar /app/app.jar

# Create the data folder for local H2 database storage
RUN mkdir -p /app/data

# Default port, which Render will override via PORT environment variable
ENV PORT=9090
EXPOSE 9090

# Run the Spring Boot application, dynamically binding to Render's port
CMD ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT}"]
