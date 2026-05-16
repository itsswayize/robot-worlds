FROM eclipse-temurin:21-jre

# Only copy the fat jar.
COPY target/*-jar-with-dependencies.jar /app.jar

EXPOSE 5050

# FIX: You MUST include the email property, or the server will crash on startup.
ENTRYPOINT ["java", "-Duser.email=docker@example.com", "-jar", "/app.jar"]