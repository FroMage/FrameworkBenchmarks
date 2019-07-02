FROM maven:3.6.1-jdk-11-slim as maven
WORKDIR /quarkus-pg-client
COPY pom.xml pom.xml
RUN mvn dependency:go-offline -q
COPY src src
RUN mvn package -q

FROM openjdk:11.0.3-jdk-slim
WORKDIR /quarkus-pg-client
COPY --from=maven /quarkus-pg-client/target/lib lib
COPY --from=maven /quarkus-pg-client/target/benchmark-pg-client-1.0-SNAPSHOT-runner.jar app.jar
CMD ["java", "-server", "-XX:+UseNUMA", "-XX:+UseParallelGC", "-jar", "app.jar"]
