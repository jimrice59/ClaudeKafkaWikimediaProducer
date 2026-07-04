FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy POMs first so dependency resolution is cached independently of source changes
COPY pom.xml .
COPY kafka-producer/pom.xml kafka-producer/
COPY kafka-consumer/pom.xml kafka-consumer/
RUN mvn dependency:go-offline -pl kafka-producer -am -q

COPY kafka-producer/src kafka-producer/src
RUN mvn package -pl kafka-producer -am -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/kafka-producer/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
