# Coffee Bean Inventory System - Docker runtime
FROM eclipse-temurin:17-jre

# Run as non-root (good practice)
RUN useradd -ms /bin/bash appuser
USER appuser

WORKDIR /app


ARG JAR_FILE=target/app.jar
COPY ${JAR_FILE} app.jar


EXPOSE 8080


ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java","-jar","/app/app.jar"]
