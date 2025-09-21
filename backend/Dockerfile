FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY frontend/pom.xml frontend/pom.xml
COPY backend/pom.xml backend/pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY . .
RUN mvn -q -DskipTests -pl backend -am package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/backend/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]