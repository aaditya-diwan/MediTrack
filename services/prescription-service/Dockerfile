FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -Ddependency-check.skip=true -B

# glibc JRE (not Alpine/musl): snappy-java's native Kafka compression lib is
# glibc-linked and fails to load on Alpine, breaking event publishing.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser
COPY --from=build /app/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1
EXPOSE 8086
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
