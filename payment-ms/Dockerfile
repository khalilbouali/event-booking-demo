# =========================================================
# Build stage
# Maven + JDK 25
# =========================================================
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy pom first for dependency caching
COPY pom.xml .

RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B


# =========================================================
# Runtime stage
# JRE 25 only
# =========================================================
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]