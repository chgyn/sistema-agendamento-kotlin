# 💈 BarberFlow — Plataforma Monolítica de Agendamento para Barbearias e Salões de Beleza

BarberFlow é uma aplicação web monolítica multi-tenant desenvolvida com **Kotlin 2.1 / Ktor 3.1**, **Vue 3 / Vite** e **PostgreSQL 16**, projetada sob os princípios da **Clean Architecture (Hexagonal)**, alta concorrência com Coroutines e garantia física de prevenção de *double-booking* através de *Exclusion Constraints* no banco de dados.

---

## 🚀 Tecnologias Utilizadas

- **Backend:**
  - Kotlin 2.1.10
  - Ktor 3.1.0 (Engine Netty, Content Negotiation, StatusPages, JWT Auth, CORS)
  - JetBrains Exposed ORM 0.57.0 (SQL DSL & DAO)
  - HikariCP 6.2.1 (Connection Pool de alta performance)
  - Flyway 10.22.0 (Versionamento automático de banco de dados)
  - Koin 4.0.1 (Injeção de Dependências idiomática)
  - Kotlin Coroutines & Channels (Structured Concurrency e Background Workers)
  - BCrypt 0.4 (Hash de senhas)
  - JUnit 5 & MockK 1.13.14 (Testes automatizados com isolamento absoluto)
- **Frontend:**
  - Vue 3 (`<script setup>`, Composition API)
  - Vite 6
  - Tailwind CSS v4 (Tema Dark Glassmorphism)
  - Pinia 2.3 (Gerenciamento de estado global)
  - Vue Router 4 (Roteamento SPA e route guards)
  - Axios 1.7 (Cliente HTTP com interceptors de JWT)
- **Infraestrutura & Banco:**
  - PostgreSQL 16 com extensões `uuid-ossp` e `btree_gist`
  - Redis 7 Alpine
  - Docker & Docker Compose (Multi-stage build unificado)

---

## 📋 Principais Funcionalidades

1. **Multi-Tenancy por Barbearia/Salão:**
   - Suporte a múltiplos estabelecimentos com URLs dedicadas (`/b/:slug`).
   - Perfis de acesso isolados: `SUPER_ADMIN`, `TENANT_ADMIN` e `OPERATOR`.
2. **Portal de Agendamento Público:**
   - Acesso sem necessidade de cadastro/login prévio.
   - Seleção de serviço, profissional, calendário de datas e consulta de horários disponíveis em tempo real.
   - Emissão de link e token criptográfico seguro para cancelamento imediato sem login.
3. **Zero Double-Booking:**
   - Algoritmo de cálculo de slots que desconta almoços, folgas e agendamentos existentes.
   - Constraint de exclusão no PostgreSQL (`EXCLUDE USING gist`) garantindo atomicidade absoluta no banco.
4. **Painel Operacional do Estabelecimento:**
   - Quadro diário de agendamentos com mudança rápida de status (`Confirmado`, `Concluído`, `Cancelado`).
   - Link direto para contato via WhatsApp do cliente.
   - Agendamento de balcão para clientes presenciais ou telefônicos.
   - Gestão de barbeiros e editor interativo da grade de horários semanais.
   - Catálogo de serviços e precificação.

---

## 🏃 Como Rodar a Aplicação

### Com Docker Compose (Recomendado)
Para subir o banco de dados PostgreSQL, o Redis e a aplicação monolítica com um único comando:

```bash
docker compose up --build
```

A aplicação estará disponível em:
- **Página de Agendamento Público:** [http://localhost:8080/b/barbearia-vintage](http://localhost:8080/b/barbearia-vintage)
- **Painel Administrativo:** [http://localhost:8080/login](http://localhost:8080/login)

### Credenciais Padrão (Seed Data)
- **Super Administrador:** `superadmin@barbershop.com` | Senha: `admin123`
- **Dono Barbearia Vintage:** `admin@vintageclub.com` | Senha: `admin123`
- **Recepção/Operador:** `operador@vintageclub.com` | Senha: `admin123`

---

## 🧪 Testes Automatizados

Os testes do projeto utilizam MockK para garantir 100% de isolamento sem tocar em bancos de dados reais de desenvolvimento:

```bash
# Executar todos os testes unitários
./gradlew test
```

---

## 📚 Documentação Técnica Completa

Para detalhes de endpoints REST, payloads JSON, arquitetura detalhada e workers assíncronos, consulte:
- [Documentação Técnica do Módulo de Agendamentos](docs/MODULO_AGENDAMENTO_BARBEARIAS.md)
