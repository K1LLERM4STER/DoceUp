FROM node:22-bookworm-slim AS frontend
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.16-eclipse-temurin-21 AS backend
WORKDIR /build
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=frontend /build/dist ./src/main/resources/static
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /build/target/doceup-0.1.0-SNAPSHOT.jar /app/doceup.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/doceup.jar"]
