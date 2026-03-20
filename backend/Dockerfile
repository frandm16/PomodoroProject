FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY backend/ ./backend/

RUN mkdir -p frontend && \
    echo '<project><modelVersion>4.0.0</modelVersion><artifactId>frontend</artifactId><parent><groupId>com.frandm</groupId><artifactId>studytracker-parent</artifactId><version>1.0.0</version></parent></project>' > frontend/pom.xml

RUN mvn -pl backend -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]