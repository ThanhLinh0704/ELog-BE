# ── Stage 1: Build stage ──
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build application executable JAR skipping tests
RUN mvn clean package -DskipTests

# ── Stage 2: Runtime stage ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install tzdata for timezone support
RUN apk add --no-cache tzdata
ENV TZ=Asia/Ho_Chi_Minh

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Active profile defaults to prod
ENV SPRING_PROFILES_ACTIVE=prod

# Run the executable JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
