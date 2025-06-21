# Stage 1: Build the Spring Boot app using official Maven with Java 21
FROM maven:3.9.4-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml and go offline (download dependencies)
COPY backend/pom.xml .
#RUN mvn dependency:go-offline

# Copy full source and build, skipping tests
COPY backend/src ./src
RUN mvn package -DskipTests

# Stage 2: Run the app with lightweight Java runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
