FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package \
    -Dmaven.test.skip=true \
    -Ddependency-check.skip=true \
    -B

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd -r appgroup && \
    useradd -r -g appgroup appuser

COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE 8081

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]