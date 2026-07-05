# Multi-stage Dockerfile for Insurance Service
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source, then build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -Ddependency-check.skip=true -B

# Stage 2: Runtime
# glibc JRE (not Alpine/musl): snappy-java's native Kafka compression lib is
# glibc-linked and fails to load on Alpine, breaking event publishing.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Add non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

# Copy artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Expose port
EXPOSE 8083

# JVM options for containerized environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
