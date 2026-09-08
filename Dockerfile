FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --create-home --home-dir /home/spring spring

COPY --from=builder /workspace/target/*.jar /app/app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
