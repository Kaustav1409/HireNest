# Stage 1: Build
FROM maven:3.8.8-eclipse-temurin-17-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY libs ./libs
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /build/target/hirenest-backend-0.0.1-SNAPSHOT.jar /app/app.jar

# Persistent H2 storage directory
RUN mkdir -p /app/data

# Render injects PORT at runtime; default to 8080 so local runs work too
ENV PORT=8080
EXPOSE 8080

# Use shell form so ${PORT} expands correctly at container start
CMD ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT}"]
