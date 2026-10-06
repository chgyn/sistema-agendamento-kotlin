# 📖 Módulo: Sistema Multi-Tenant de Agendamento para Barbearias e Salões de Beleza (BarberFlow)

Este documento detalha a arquitetura, regras de negócio, contratos de API REST, modelagem de dados, concorrência e guia de desenvolvimento do sistema de agendamentos.

---

## 1. 🏗️ Visão Geral da Arquitetura

O sistema é construído no padrão **Monolito Modular / Clean Architecture (Hexagonal)** com suporte a múltiplos estabelecimentos (Multi-Tenancy) e isolamento em containers Docker:

- **Backend:** Kotlin 2.1+, Ktor 3.1.x, JetBrains Exposed ORM (SQL DSL), HikariCP, Flyway, Koin 4.x, BCrypt e Structured Concurrency em Coroutines.
- **Frontend:** Vue 3 (`<script setup>`, Composition API), Vite, Tailwind CSS v4, Pinia, Vue Router 4, Axios.
- **Banco de Dados & Concorrência:** PostgreSQL 16 com extensão `btree_gist` e exclusion constraint `no_overlapping_appointments` para prevenção física de *double-booking* de horários.
- **Cache & Eventos:** Redis 7 / Kotlin Coroutine Channels para background workers.
- **Distribuição:** Monolito autossuficiente onde o Ktor entrega tanto as rotas de API `/api/v1/...` quanto os artefatos estáticos da SPA Vue 3 via fallback de roteamento.

```
                           +---------------------------+
                           |     Cliente / Navegador    |
                           +-------------+-------------+
                                         |
                                         v
                         +---------------+---------------+
                         |   Ktor 3.1 Engine (Port 8080) |
                         +---------------+---------------+
                                 |               |
                 [SPA Fallback]  |               |  [REST API: /api/v1]
                                 v               v
                        +----------------+  +--------------------+
                        | Vue 3 Frontend |  | Use Cases (Clean)  |
                        +----------------+  +---------+----------+
                                                      |
                                      +---------------+---------------+
                                      |                               |
                                      v                               v
                            +--------------------+        +-----------------------+
                            | Exposed Repository |        | Notification Worker   |
                            +---------+----------+        +-----------+-----------+
                                      |                               |
                                      v                               v
                            +--------------------+        +-----------------------+
                            |   PostgreSQL 16    |        | Redis / Channels Coro |
                            |  (Exclusion GIST)  |        +-----------------------+
                            +--------------------+
```

---

## 2. 🔐 Autenticação, Perfis & Multi-Tenancy

O sistema adota o modelo multi-tenant baseado em tenant discriminator com chave estrangeira `tenant_id`:

| Papel (Role) | Escopo de Acesso | Permissões |
| :--- | :--- | :--- |
| **`SUPER_ADMIN`** | Global (Todos os Tenants) | Visualizar e cadastrar novas barbearias (`POST /api/v1/admin/tenants`), métricas gerais da plataforma. |
| **`TENANT_ADMIN`** | Barbearia Vinculada (`tenant_id`) | Gerenciar dados do salão, cadastrar e precificar serviços, cadastrar barbeiros e configurar suas grades de horários semanais. |
| **`OPERATOR`** | Barbearia Vinculada (`tenant_id`) | Acessar o quadro diário de atendimentos, alterar status (Confirmar, Concluir, Cancelar) e realizar agendamento manual de balcão. |
| **`CLIENTE PÚBLICO`** | Página Aberta (`/b/:slug`) | Consulta aberta sem login prévio. Permite agendar horários livres e cancelar agendamentos via token exclusivo. |

### Contas Padrão Pré-configuradas (Seed Data)
- **Super Administrador:** `superadmin@barbershop.com` | Senha: `admin123`
- **Dono Barbearia Vintage:** `admin@vintageclub.com` | Senha: `admin123`
- **Recepção/Operador Vintage:** `operador@vintageclub.com` | Senha: `admin123`
- **URL Pública Demonstrativa:** `/b/barbearia-vintage`

---

## 3. 🛡️ Concorrência e Prevenção de Double-Booking

Para erradicar qualquer possibilidade de dois clientes reservarem o mesmo barbeiro no mesmo horário, foi implementada uma estratégia em duas camadas:

1. **Camada de Aplicação (`CalculateAvailableSlotsUseCase` & `CreatePublicAppointmentUseCase`):**
   - Verificação em memória dos slots livres considerando o expediente do barbeiro, intervalos de almoço, folgas/bloqueios e agendamentos já confirmados/agendados.
   - Chamada a `appointmentRepository.countOverlapping(profId, start, end)` antes de inserir.
2. **Camada de Banco de Dados (PostgreSQL Exclusion Constraint):**
   - Definida em `src/main/resources/db/migration/V1__init_schema.sql`:
   ```sql
   ALTER TABLE appointments
       ADD CONSTRAINT no_overlapping_appointments
       EXCLUDE USING gist (
           professional_id WITH =,
           tstzrange(start_time, end_time, '[)') WITH &&
       ) WHERE (status IN ('SCHEDULED', 'CONFIRMED'));
   ```
   Se duas requisições simultâneas chegarem no mesmo milissegundo, a segunda será rejeitada pelo PostgreSQL e tratada pelo `StatusPages` do Ktor retornando `HTTP 409 Conflict` com mensagem amigável ao cliente.

---

## 4. 📖 Catálogo de Endpoints REST (Ktor)

### 4.1 Rotas Públicas (Sem Autenticação)

#### Obter Detalhes do Estabelecimento
- **GET** `/api/v1/public/tenants/{slug}`
- **Parâmetros:** `slug` (String, no path)
- **Resposta Sucesso (200 OK):**
```json
{
  "id": "7b049d5a-8b1e-4537-8854-47cb3214a1a5",
  "name": "Barbearia Vintage Club",
  "slug": "barbearia-vintage",
  "phone": "(11) 98888-7777",
  "email": "contato@vintageclub.com",
  "timezone": "America/Sao_Paulo",
  "services": [
    {
      "id": "d04a6c42-2b63-44f6-8c4d-b0ad52f82163",
      "name": "Corte Clássico & Degradê",
      "description": "Corte completo com tesoura e máquina.",
      "durationMinutes": 30,
      "price": 45.0
    }
  ],
  "professionals": [
    {
      "id": "e6a0d0fb-b78c-4cb0-a8dc-192cb2ea946c",
      "name": "Carlos Navalha",
      "bio": "Especialista em cortes clássicos.",
      "serviceIds": ["d04a6c42-2b63-44f6-8c4d-b0ad52f82163"]
    }
  ]
}
```

#### Consultar Disponibilidade de Horários Livres (Slots)
- **GET** `/api/v1/public/tenants/{slug}/availability?serviceId={uuid}&date=YYYY-MM-DD[&professionalId={uuid}]`
- **Query Params:**
  - `serviceId`: UUID do serviço selecionado (obrigatório).
  - `date`: Data do agendamento formato `YYYY-MM-DD` (obrigatório).
  - `professionalId`: UUID do profissional (opcional; se omitido, busca horários livres de qualquer profissional habilitado).
- **Resposta Sucesso (200 OK):**
```json
[
  {
    "startTime": "2026-10-15T09:00:00-03:00",
    "endTime": "2026-10-15T09:30:00-03:00",
    "professionalId": "e6a0d0fb-b78c-4cb0-a8dc-192cb2ea946c"
  },
  {
    "startTime": "2026-10-15T09:30:00-03:00",
    "endTime": "2026-10-15T10:00:00-03:00",
    "professionalId": "e6a0d0fb-b78c-4cb0-a8dc-192cb2ea946c"
  }
]
```

#### Realizar Agendamento Público
- **POST** `/api/v1/public/tenants/{slug}/appointments`
- **Payload Request:**
```json
{
  "professionalId": "e6a0d0fb-b78c-4cb0-a8dc-192cb2ea946c",
  "serviceId": "d04a6c42-2b63-44f6-8c4d-b0ad52f82163",
  "startTime": "2026-10-15T14:30:00-03:00",
  "customerName": "Rodrigo Silva",
  "customerEmail": "rodrigo@email.com",
  "customerPhone": "(11) 98765-4321",
  "notes": "Preferência por corte com tesoura no topo"
}
```
- **Resposta Sucesso (201 Created):**
```json
{
  "id": "bfd6e372-5b92-4e42-b062-0e3636599b4d",
  "customerName": "Rodrigo Silva",
  "customerEmail": "rodrigo@email.com",
  "customerPhone": "(11) 98765-4321",
  "serviceName": "Corte Clássico & Degradê",
  "professionalName": "Carlos Navalha",
  "startTime": "2026-10-15T14:30:00-03:00",
  "endTime": "2026-10-15T15:00:00-03:00",
  "status": "SCHEDULED",
  "cancellationToken": "c76f183769941a8e1b12b5cd367243c4819df511ef9cb281"
}
```

#### Consultar Agendamento por Token
- **GET** `/api/v1/public/appointments/{token}`
- **Resposta Sucesso (200 OK):**
```json
{
  "id": "bfd6e372-5b92-4e42-b062-0e3636599b4d",
  "tenantName": "Barbearia Vintage Club",
  "serviceName": "Corte Clássico & Degradê",
  "professionalName": "Carlos Navalha",
  "customerName": "Rodrigo Silva",
  "startTime": "2026-10-15T14:30:00-03:00",
  "endTime": "2026-10-15T15:00:00-03:00",
  "status": "SCHEDULED"
}
```

#### Cancelar / Desmarcar Agendamento por Token
- **POST** `/api/v1/public/appointments/{token}/cancel`
- **Resposta Sucesso (200 OK):**
```json
{
  "message": "Agendamento cancelado com sucesso."
}
```

---

### 4.2 Rotas Administrativas (Requer Cabeçalho `Authorization: Bearer <JWT>`)

| Método | Endpoint URI | Role Mínima | Descrição |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Pública | Login com email/senha e emissão de JWT. |
| `GET` | `/api/v1/auth/me` | Autenticado | Dados do usuário logado e tenant vinculado. |
| `GET` | `/api/v1/admin/tenants` | `SUPER_ADMIN` | Listagem de todos os estabelecimentos da plataforma. |
| `POST` | `/api/v1/admin/tenants` | `SUPER_ADMIN` | Cadastro de nova barbearia e seu administrador inicial. |
| `GET` | `/api/v1/admin/appointments` | `OPERATOR`, `TENANT_ADMIN` | Listagem dos atendimentos do dia com filtro por data. |
| `PATCH` | `/api/v1/admin/appointments/{id}/status` | `OPERATOR`, `TENANT_ADMIN` | Atualiza status: `CONFIRMED`, `COMPLETED` ou `CANCELLED`. |
| `GET` | `/api/v1/admin/professionals` | `OPERATOR`, `TENANT_ADMIN` | Listagem dos profissionais do estabelecimento. |
| `POST` | `/api/v1/admin/professionals` | `TENANT_ADMIN` | Cadastra novo profissional e vincula serviços. |
| `GET` | `/api/v1/admin/professionals/{id}/schedule` | `TENANT_ADMIN` | Obtém grade semanal de horários do profissional. |
| `PUT` | `/api/v1/admin/professionals/{id}/schedule` | `TENANT_ADMIN` | Atualiza grade semanal e pausas de almoço. |
| `GET` | `/api/v1/admin/services` | `OPERATOR`, `TENANT_ADMIN` | Catálogo de serviços do estabelecimento. |
| `POST` | `/api/v1/admin/services` | `TENANT_ADMIN` | Criação de novo serviço e precificação. |

---

## 5. 🔄 Tarefas Assíncronas & Workers de Corrotinas

- **Componente:** `AsyncNotificationService` (`infrastructure/jobs/NotificationWorker.kt`)
- **Funcionamento:** Utiliza Structured Concurrency com `CoroutineScope(Dispatchers.Default + SupervisorJob())` e `Channel<NotificationEvent>(capacity = Channel.BUFFERED)`.
- **Eventos Processados:**
  - `NotificationEvent.AppointmentCreated`: Dispara e-mail contendo resumo do horário e link seguro para cancelamento.
  - `NotificationEvent.AppointmentCancelled`: Dispara notificação de cancelamento confirmado.
- **Idempotência & Isolamento:** O processamento em background não bloqueia o ciclo da requisição HTTP do Ktor e captura exceções sem interromper a execução do worker.

---

## 6. 🚀 Como Executar o Projeto

### Opção A: Execução Completa via Docker Compose (Recomendada)
```bash
# Sobe PostgreSQL 16 (com btree_gist), Redis 7 e App Monolito (Ktor + Vue 3 embutido)
docker compose up --build
```
Acesse a aplicação no navegador em:
- **Área Pública de Agendamento:** `http://localhost:8080/b/barbearia-vintage`
- **Painel Administrativo:** `http://localhost:8080/login`

### Opção B: Execução em Modo de Desenvolvimento Local

1. **Subir Banco de Dados e Redis:**
```bash
docker compose up -d postgres redis
```

2. **Frontend Vue 3 (Vite Dev Server com Hot-Reload):**
```bash
cd frontend
npm install
npm run dev
# Acesse em http://localhost:3000 (com proxy configurado para :8080)
```

3. **Backend Kotlin/Ktor:**
```bash
# Executar a API Ktor
./gradlew run

# Executar Testes Unitários Isolados (MockK)
./gradlew test
```
