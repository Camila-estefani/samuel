# Etapa 1: Build
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Imagen final liviana
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/pedidos-service-1.0.0.jar app.jar
ENV SERVER_PORT=8081
ENV MENU_SERVICE_URL=http://menu-service:8080
ENV MENU_TIMEOUT_SECONDS=3
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
