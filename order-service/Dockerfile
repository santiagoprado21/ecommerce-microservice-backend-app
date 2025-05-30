FROM openjdk:17-jdk-slim

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=dev

COPY build/libs/*.jar order-service.jar

EXPOSE 8800

ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "order-service.jar"]
