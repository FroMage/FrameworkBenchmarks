FROM maven:3.6.3-jdk-11-slim as maven
WORKDIR /quarkus
COPY pom.xml pom.xml
COPY base/pom.xml base/pom.xml
COPY hibernate/pom.xml hibernate/pom.xml
COPY hibernate-reactive/pom.xml hibernate-reactive/pom.xml
COPY pgclient/pom.xml pgclient/pom.xml
RUN mkdir -p /root/.m2/repository/io
#RUN mkdir -p /root/.m2/repository/org/jboss
COPY m2-quarkus /root/.m2/repository/io/quarkus
#COPY m2-smallrye /root/.m2/repository/io/smallrye
#COPY m2-vertx /root/.m2/repository/io/vertx
#COPY m2-hibernate /root/.m2/repository/org/hibernate
#COPY m2-resteasy /root/.m2/repository/org/jboss/resteasy
#COPY m2-narayana /root/.m2/repository/org/jboss/narayana

RUN mvn dependency:go-offline -q -pl base

COPY base/src base/src
COPY hibernate/src hibernate/src
COPY hibernate-reactive/src hibernate-reactive/src
COPY pgclient/src pgclient/src

RUN mvn package -q -pl hibernate-reactive -am

FROM openjdk:11.0.6-jdk-slim
WORKDIR /quarkus
COPY --from=maven /quarkus/hibernate-reactive/target/lib lib
COPY --from=maven /quarkus/hibernate-reactive/target/hibernate-reactive-1.0-SNAPSHOT-runner.jar app.jar
CMD ["java", "-server", "-XX:+UseNUMA", "-XX:+UseParallelGC", "-Djava.lang.Integer.IntegerCache.high=10000", "-Dvertx.disableHttpHeadersValidation=true", "-Dvertx.disableMetrics=true", "-Dvertx.disableH2c=true", "-Dvertx.disableWebsockets=true", "-Dvertx.flashPolicyHandler=false", "-Dvertx.threadChecks=false", "-Dvertx.disableContextTimings=true", "-Dvertx.disableTCCL=true", "-Dhibernate.allow_update_outside_transaction=true", "-Djboss.threads.eqe.statistics=false", "-jar", "app.jar"]