FROM eclipse-temurin:21-jre

WORKDIR /app
COPY MyContainerApp.jar .

# Show JVM settings and run app
CMD ["java", "-XshowSettings:vm", "-jar", "MyContainerApp.jar"]
