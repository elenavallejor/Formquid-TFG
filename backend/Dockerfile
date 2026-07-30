# Etapa 1 - Compilación

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw

# Descarga dependencias 
RUN ./mvnw dependency:go-offline

COPY src src

# Compilamos 
RUN ./mvnw clean package -DskipTests


# Etapa 2 - Ejecución

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
