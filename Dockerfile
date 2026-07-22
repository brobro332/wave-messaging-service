FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/libs/wave-messaging-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
