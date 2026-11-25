# Etapa 1: compilar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Etapa 2: ejecutar con un usuario sin privilegios
FROM eclipse-temurin:17-jre
RUN useradd --system --create-home --shell /usr/sbin/nologin taller
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/datos/fotos /app/datos/respaldos && chown -R taller:taller /app/datos
USER taller
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75" \
    TALLER_FOTOS_DIR=/app/datos/fotos \
    TALLER_RESPALDOS_DIR=/app/datos/respaldos
ENTRYPOINT ["java", "-jar", "app.jar"]
