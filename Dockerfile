# ==========================================
# Estágio 1: Build do Frontend (Vue 3 + Vite)
# ==========================================
FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ==========================================
# Estágio 2: Build do Backend (Kotlin / Ktor)
# ==========================================
FROM gradle:8.12-jdk21-alpine AS backend-builder
WORKDIR /app

# Copia configurações Gradle para cache de dependências
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Copia código fonte do backend
COPY src ./src

# Embutir os arquivos estáticos do frontend na pasta de resources do Ktor
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

# Build da distribuição da aplicação
RUN gradle installDist --no-daemon -x test

# ==========================================
# Estágio 3: Runtime da Imagem Final
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Criar usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=backend-builder /app/build/install/barbershop-monolith ./

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["./bin/barbershop-monolith"]
