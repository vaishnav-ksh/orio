# Multi-stage Dockerfile for Spring Boot Application
# Stage 1: Build the JAR
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create directory for persistent SQLite database
RUN mkdir -p /app/data

# Copy built JAR from builder stage
COPY --from=builder /app/target/product-inventory-management-1.0.0.jar app.jar

# Expose server port
EXPOSE 8080

# Environment variables
ENV PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx384m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
