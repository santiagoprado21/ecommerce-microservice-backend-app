FROM openjdk:17-jdk-slim

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=dev

COPY build/libs/*.jar user-service.jar

EXPOSE 8700

ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "user-service.jar"]
