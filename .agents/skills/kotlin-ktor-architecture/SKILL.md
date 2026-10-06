---
name: kotlin-ktor-architecture
description: Diretrizes de arquitetura Clean/Hexagonal, boas práticas Kotlin 2.x, Ktor 3.x, Coroutines, Exposed ORM, HikariCP, Flyway, concorrência, criptografia AES-256-GCM e Vue 3 para projetos Kotlin/Ktor.
---

# 📐 Skill: Kotlin / Ktor Clean Architecture & Best Practices Standard

Você é um especialista em engenharia de software com foco no ecossistema Kotlin (2.x), Ktor (3.x), Clean Architecture, Exposed ORM, HikariCP, Flyway, Coroutines & Structured Concurrency, PostgreSQL, Redis e Vue 3. Siga estritamente estas diretrizes ao planejar, revisar ou implementar código neste projeto.

---

## 1. Princípios de Kotlin Moderno (Kotlin 2.x & Coroutines)

- **Tratamento Idiomático de Erros:**
  - NUNCA engula exceções silenciosamente com blocos `catch (e: Exception) {}` vazios.
  - Utilize `sealed interface` ou `sealed class` para representar resultados de negócio e falhas conhecidas (`sealed interface Result<out T>`), permitindo checagem exaustiva com `when`.
  - Dispare exceções de domínio tipadas (ex: `EntityNotFoundException`, `UnauthorizedBusinessException`) e configure o plugin `StatusPages` do Ktor para mapeá-las em respostas HTTP padronizadas com o status code correto (400, 401, 403, 404, 409).
- **Tipagem Forte, Imutabilidade e Null Safety:**
  - Prefira sempre `val` em vez de `var`. Entidades e DTOs devem ser `data class` imutáveis.
  - Respeite o sistema de tipos de nulidade do Kotlin. NUNCA use o operador `!!` a menos que seja estritamente garantido por um teste ou invariante comprovável. Use `?.let`, `?:` (Elvis operator) ou smart casts.
  - Anote DTOs com `@Serializable` do `kotlinx.serialization`. Use `@Transient` para campos confidenciais (ex: senhas, hashes, chaves criptográficas não processadas).
- **Concorrência e Ciclo de Vida Seguros (Structured Concurrency):**
  - Todas as funções de I/O, banco de dados ou chamadas externas de rede DEVEM ser declaradas com `suspend`.
  - NUNCA utilize `GlobalScope.launch`. Sempre amarre a execução a um `CoroutineScope` com ciclo de vida controlado e `SupervisorJob()`.
  - Chamadas de I/O bloqueante (como drivers JDBC legados ou chamadas de disco) DEVEM rodar em `withContext(Dispatchers.IO)` ou `newSuspendedTransaction(Dispatchers.IO)`.
  - Proteja estados mutáveis compartilhados entre corrotinas com `kotlinx.coroutines.sync.Mutex` em vez de blocos sincronizados primitivos de Java (`synchronized`).

---

## 2. Clean Architecture & Estrutura de Pacotes

```text
src/
├── main/
│   ├── kotlin/
│   │   └── com/empresa/projeto/
│   │       ├── Application.kt              # Entrypoint Ktor (embeddedServer ou EngineMain)
│   │       ├── plugins/                    # Plugins Ktor configurados
│   │       │   ├── Routing.kt              # Registro central de rotas HTTP
│   │       │   ├── Serialization.kt        # ContentNegotiation com kotlinx.serialization
│   │       │   ├── Security.kt             # JWT Authentication (ktor-server-auth-jwt)
│   │       │   ├── StatusPages.kt          # Tratamento global de exceções
│   │       │   └── DependencyInjection.kt  # Koin modules e injeção
│   │       ├── domain/                     # Camada de Domínio Pura (zero dependência de frameworks)
│   │       │   ├── model/                  # Entidades de Domínio puras (data classes) e Value Objects
│   │       │   ├── ports/                  # Interfaces de Repositórios e Provedores Externos
│   │       │   └── errors/                 # Exceções e Sealed Classes de Erros de Domínio
│   │       ├── application/                # Camada de Aplicação / Casos de Uso
│   │       │   ├── dtos/                   # Request & Response DTOs (@Serializable)
│   │       │   └── usecases/               # Classes de Caso de Uso que orquestram a regra de negócio
│   │       ├── infrastructure/             # Adaptadores de Infraestrutura (banco, redis, cripto, clientes)
│   │       │   ├── database/               # Configuração do Exposed, HikariCP e tabelas
│   │       │   │   ├── DatabaseFactory.kt
│   │       │   │   └── tables/             # Tabelas Exposed (Table, UUIDTable, IdTable)
│   │       │   ├── repositories/           # Implementações Exposed das interfaces de repositório
│   │       │   ├── security/               # Criptografia AES-256-GCM e PasswordHasher (BCrypt)
│   │       │   ├── jobs/                   # Background workers (Coroutines / Redis)
│   │       │   └── integrations/           # Clientes HTTP externos (Ktor Client CIO)
│   │       └── presentation/               # Camada HTTP Ktor
│   │           └── routes/                 # Extension functions de rotas (Route.userRoutes())
│   └── resources/
│       ├── application.conf                # Configurações HOCON do Ktor 3, DB, Redis e JWT
│       ├── logback.xml                     # Configurações de log SLF4J / Logback
│       └── db/migration/                   # Scripts SQL versionados do Flyway (V1__init.sql)
└── test/
    └── kotlin/
        └── com/empresa/projeto/
            ├── routes/                     # Testes de integração de API (ktor-server-test-host)
            ├── usecases/                   # Testes unitários de regras de negócio (MockK)
            └── repositories/               # Testes de repositório (Testcontainers PostgreSQL / H2)
```

### 2.1 Especificidades Mandatórias do Ktor 3.x
- **Configuração HOCON (`application.conf`):** O Ktor 3.x utiliza por padrão o formato HOCON (`src/main/resources/application.conf`). Nunca utilize `application.yaml` sem a inclusão explícita de `ktor-server-config-yaml`. Variáveis de ambiente devem seguir a sintaxe HOCON `${?ENV_VAR}`.
- **Injeção Koin Desacoplada no Ktor 3 (`koinInject`):**
  - No Ktor 3.x, a arquitetura interna do roteamento foi reestruturada (`RoutingKt` removido em favor de `RoutingRoot`/`RoutingNode`).
  - O uso direto de `Route.inject<T>()` do `koin-ktor` causa `java.lang.NoClassDefFoundError: io/ktor/server/routing/RoutingKt`.
  - **Padrão Obrigatório:** Em arquivos de rotas (`Route.xxxRoutes()`), utilize `val useCase by koinInject<MyUseCase>()` (que invoca `org.koin.core.context.GlobalContext.get().get<T>()`) ou injeção desacoplada.
- **Serialização Estrita com Kotlinx Serialization no Ktor 3:**
  - O Ktor 3 proíbe serializar coleções heterogêneas sem tipos registrados (`mapOf("status" to "OK", "timestamp" to 123L)` falha em runtime).
  - **Padrão Obrigatório:** Todas as respostas em `call.respond()` DEVEM utilizar `data class` anotadas com `@Serializable` (ex: `MessageResponse`, `HealthStatusResponse`, DTOs de domínio), NUNCA `mapOf<String, Any>`.
- **Imports Explícitos de Roteamento:**
  - Em arquivos de rotas Ktor 3, sempre inclua explicitamente `import io.ktor.server.routing.*` para garantir que `get`, `post`, `put`, `delete`, `route` e `call` sejam resolvidos sem ambiguidade.
- **Servir Artefatos Estáticos da SPA Vue 3:**
  - Utilize `staticResources("/", "static") { default("index.html") }` para servir arquivos embutidos e configure fallback para rotas que não pertençam à API (`/api/`).

### 2.2 Rotas Enxutas no Ktor (Thin Routes)
- Extension functions de rotas têm a responsabilidade estrita de:
  1. Validar e desserializar o payload JSON: `val req = call.receive<CreateItemRequest>()`
  2. Extrair credenciais do usuário autenticado: `val principal = call.principal<JWTPrincipal>()`
  3. Invocar o Use Case injetado: `val result = useCase.execute(req)`
  4. Responder com status HTTP padronizado e DTO `@Serializable`: `call.respond(HttpStatusCode.Created, result)`
- **Proibido em Rotas:** Lógica de negócio pesada, queries Exposed diretas ou chamadas bloqueantes de I/O.

### 2.3 Repositórios Exposed & Prevenção de N+1
- Métodos de repositório devem executar operações de banco sempre encapsuladas em `newSuspendedTransaction(Dispatchers.IO) { ... }`.
- **Prevenção de N+1:**
  - Ao utilizar Exposed DAO, carregue relacionamentos explicitamente com `.load(...)` ou `.with(...)` para que sejam buscados em queries otimizadas (`IN (...)`) em vez de consultas individuais repetidas.
  - Ao utilizar Exposed SQL DSL, projete queries com `innerJoin(...)` ou `leftJoin(...)` selecionando as colunas necessárias em um único comando SQL.
- **Transações Atômicas:**
  ```kotlin
  suspend fun executeAtomicOperation(data: ItemData): Item = newSuspendedTransaction(Dispatchers.IO) {
      val item = ItemEntity.new { ... }
      LogTable.insert { ... }
      item.toDomain()
  }
  ```
  Se qualquer exceção for lançada dentro do bloco, o rollback é efetuado automaticamente.

---

## 3. Segurança & Criptografia (AES-256-GCM + BCrypt)

- **Criptografia Simétrica (AES-256-GCM):**
  - Todas as credenciais de terceiros, tokens de API e cookies confidenciais devem ser cifrados com AES-256-GCM (`javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")`) com IV aleatório de 12 bytes gerado por `SecureRandom` e tag de autenticação de 128 bits.
  - O IV deve ser prefixado aos bytes criptografados antes de salvar no banco ou codificar em Base64.
- **Hashes de Senha:**
  - Senhas de usuários DEVEM ser tratadas com BCrypt (custo 12) ou Argon2. Nunca armazene senhas com MD5, SHA-1 ou SHA-256 sem salt.
- **Mascaramento:**
  - Campos sensíveis DEVEM conter `@Transient` do `kotlinx.serialization` para garantir que respostas JSON nunca exponham senhas ou tokens internos.

---

## 4. Tarefas Assíncronas (Coroutines, Channels & Redis)

- **Desacoplamento:** Qualquer operação de longa duração (envio de e-mails/mensagens, processamento pesado, integrações com IA, web scraping) DEVE ser executada assincronamente através de background workers com corrotinas ou filas distribuídas (Redis / RabbitMQ).
- **Structured Concurrency:** Crie workers atrelados a um escopo supervisionado (`CoroutineScope(Dispatchers.Default + SupervisorJob())`).
- **Idempotência:** Garanta que execuções repetidas da mesma tarefa em caso de retries não gerem efeitos colaterais duplicados.

---

## 5. Frontend Vue 3 + Tailwind CSS v4 + Pinia

- **Composition API:** Utilize `<script setup>` em todos os componentes e telas (`frontend/src/pages/`).
- **Comunicação API:** Utilize o cliente Axios centralizado (`frontend/src/services/api.js`), que gerencia a injeção do cabeçalho `Authorization: Bearer <token>` e trata respostas `401 Unauthorized` com redirecionamento para o login.
- **Tailwind CSS v4:** Aplique classes utilitárias modernas, tema escuro consistente (dark slate / zinc), efeitos de glassmorphism (`backdrop-blur-md bg-slate-900/80 border border-slate-800`), e micro-animações em botões e cards.

---

## 6. 🚨 REGRA CRÍTICA DE ISOLAMENTO DE BANCO DE DADOS EM TESTES

- **PROIBIÇÃO ABSOLUTA**: **NUNCA** execute testes unitários ou comandos automatizados que alterem, limpem (`TRUNCATE`) ou executem drops de tabela no banco de dados principal de desenvolvimento ou produção.
- **RISCO GRAVE**: Resetar a base principal apaga dados de desenvolvimento e credenciais de integração do usuário.
- **EXECUÇÃO CORRETA DE TESTES**:
  - Em testes unitários, utilize **Mocks das interfaces** declaradas em `domain.ports` utilizando a biblioteca **MockK** (ex: `val repo = mockk<UserRepository>()`, `coEvery { repo.findById(any()) } returns user`).
  - Para testes de integração de banco de dados, utilize **EXCLUSIVAMENTE** uma base de testes dedicada, banco em memória H2 (`jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`) ou **Testcontainers PostgreSQL**.
  - **Comando Permitido**: `./gradlew test`
  - **Exemplo ESTRITAMENTE PROIBIDO**: Rodar scripts com `SchemaUtils.drop(...)` apontando para o banco de desenvolvimento ativo.
