# Build stage
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Copy Maven wrapper and POM first
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Fetch dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests

# Run stage - lightweight runtime
FROM eclipse-temurin:26-jre-alpine
WORKDIR /app
COPY --from=build /app/target/TypeAhead-0.0.1-SNAPSHOT.jar app.jar

# Optimise for low resource usage with custom JVM arguments
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=2"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
