# --- Estágio 1: Build (Compilação) ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia TODO o projeto (pom.xml, src, tudo) para dentro do container
COPY . .

# Agora sim, roda o maven. Como o pom.xml foi copiado acima, ele vai funcionar.
RUN mvn clean package -DskipTests

# --- Estágio 2: Run (Execução) ---
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Pega o .jar que foi gerado no estágio anterior
# O asterisco *.jar garante que pegue independente da versão
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]