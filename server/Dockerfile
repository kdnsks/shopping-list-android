# Этап 1: сборка исполняемого jar-файла сервера
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src
RUN gradle buildFatJar --no-daemon -q

# Этап 2: лёгкий образ только со средой выполнения Java
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/shopping-list-server-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
