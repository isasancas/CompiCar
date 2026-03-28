# --- ETAPA 1: Build del Frontend (Vite/React) ---
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
# Genera la carpeta /dist
RUN npm run build

# --- ETAPA 2: Build del Backend (Spring Boot 4 / Java 21) ---
FROM maven:3.9.6-eclipse-temurin-21 AS backend-build
WORKDIR /app

# Copiamos el pom y el código fuente del backend
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src

# TRUCO: Copiamos el build del frontend a los recursos estáticos de Spring
# Esto permite que el .jar final sirva la web de React
COPY --from=frontend-build /app/frontend/dist ./backend/src/main/resources/static

# Construimos el .jar SALTANDO los tests (porque ya pasaron en tu CI)
RUN mvn -f backend/pom.xml clean package -DskipTests

# --- ETAPA 3: Imagen de Ejecución (JRE ligera) ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copiamos el artefacto generado
COPY --from=backend-build /app/backend/target/*.jar app.jar

# Exponemos el puerto y ejecutamos
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]