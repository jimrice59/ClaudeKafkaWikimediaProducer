FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy POMs first so dependency resolution is cached independently of source changes
COPY pom.xml .
COPY kafka-producer/pom.xml kafka-producer/
COPY kafka-consumer/pom.xml kafka-consumer/
RUN mvn dependency:go-offline -pl kafka-consumer -am -q

COPY kafka-consumer/src kafka-consumer/src
RUN mvn package -pl kafka-consumer -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/kafka-consumer/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
