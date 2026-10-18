FROM node:22-bookworm-slim AS ui
WORKDIR /ui
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --ignore-scripts
COPY frontend/ ./
ARG APP_EDITION=coach
RUN test "$APP_EDITION" = coach || test "$APP_EDITION" = beta
RUN npm run build -- --mode "$APP_EDITION"

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
COPY --from=ui /ui/dist/ ./src/main/resources/static/
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends fontconfig libfreetype6 fonts-crosextra-carlito fonts-crosextra-caladea fonts-liberation2 fonts-dejavu-core && rm -rf /var/lib/apt/lists/*
RUN groupadd --gid 10001 pnb && useradd --uid 10001 --gid pnb --no-create-home pnb && mkdir -p /app /var/data && chown -R pnb:pnb /app /var/data
WORKDIR /app
COPY --from=build --chown=pnb:pnb /build/target/resume-review-0.1.0.jar ./service.jar
USER 10001:10001
ENV DATABASE_URL=jdbc:h2:file:/var/data/resume-review
ENV SERVER_ADDRESS=0.0.0.0 PORT=10000 JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -XX:MaxRAMPercentage=70"
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/service.jar"]
