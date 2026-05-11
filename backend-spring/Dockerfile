FROM gcc:14-bookworm AS c-builder
WORKDIR /build/c-engine
COPY c-engine/ ./
RUN make

FROM maven:3.9.9-eclipse-temurin-21 AS java-builder
WORKDIR /build/backend-spring

COPY backend-spring/pom.xml ./
RUN mvn -DskipTests dependency:go-offline

COPY backend-spring/src ./src
RUN mvn -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app/backend-spring

COPY --from=java-builder /build/backend-spring/target/backend-spring-0.0.1-SNAPSHOT.jar /app/backend-spring/app.jar
COPY --from=c-builder /build/c-engine/civic_alert_engine /app/c-engine/civic_alert_engine

RUN chmod +x /app/c-engine/civic_alert_engine

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/backend-spring/app.jar"]

