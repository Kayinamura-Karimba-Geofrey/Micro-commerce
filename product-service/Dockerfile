FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Run as an unprivileged user instead of root.
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --chown=app:app target/*.jar app.jar
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
