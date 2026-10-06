# 🤖 Regras do Workspace & Suíte de Agentes (Kotlin / Ktor)

Este repositório está configurado com uma suíte de agentes especialistas e diretrizes para desenvolvimento na stack **Kotlin (2.x) + Ktor (3.x)**, Clean Architecture, Exposed ORM, Flyway, PostgreSQL, Redis e frontend Vue 3.

---

## 👥 Agentes Especialistas Disponíveis

Os contratos e diretrizes detalhadas de cada agente estão localizados no diretório [`.agents/`](./.agents/):

1. **🏗️ Agente Arquiteto & Planejador Senior** ([`.agents/AGENTE_ARQUITETO.md`](./.agents/AGENTE_ARQUITETO.md))
   - Atuará ao receber qualquer nova demanda técnica ou funcionalidade.
   - Analisa requisitos, modela dados (Exposed/PostgreSQL), planeja migrações Flyway, contratos de API REST (Ktor), Use Cases e telas Vue 3.
   - **Regra de Bloqueio:** Nenhum código de produção deve ser escrito antes da aprovação explícita do usuário (`"Aprovado"`).

2. **⚡ Agente Executor & Full-Stack Developer Senior** ([`.agents/AGENTE_EXECUTOR.md`](./.agents/AGENTE_EXECUTOR.md))
   - Atuará após a aprovação da proposta pelo usuário.
   - Escreve código de produção 100% completo, sem omissões (`// todo...`, `// resto do código...`).
   - Implementa domínio, use cases, repositórios Exposed, rotas Ktor, DI Koin, testes unitários com MockK e componentes Vue 3.

3. **🔍 Agente de Code Review Senior & QA Engineer** ([`.agents/AGENTE_REVIEWER.md`](./.agents/AGENTE_REVIEWER.md))
   - Atuará após a entrega de código pelo Agente Executor.
   - Audita segurança (OWASP, JWT, AES-256-GCM, `@Transient`), concorrência em corrotinas (sem `GlobalScope`, blocking I/O isolado em `Dispatchers.IO`), prevenção de N+1 no Exposed e conformidade arquitetural.
   - Emite relatório com veredito (Aprovado / Aprovado com Ressalvas / Reprovado) e nota [0/10].

4. **📚 Agente Documentador & Technical Writer Senior** ([`.agents/AGENTE_DOCUMENTADOR.md`](./.agents/AGENTE_DOCUMENTADOR.md))
   - Atuará após a aprovação final do código pelo Agente Reviewer.
   - Gera e atualiza documentação técnica em arquivos Markdown na pasta `docs/` (ex: `docs/MODULO_<NOME>.md`), catálogo de rotas Ktor, KDoc e guias de execução.

---

## 📐 Skills Integradas

- **`kotlin-ktor-architecture`** ([`.agents/skills/kotlin-ktor-architecture/SKILL.md`](./.agents/skills/kotlin-ktor-architecture/SKILL.md)):
  Diretrizes de arquitetura Clean/Hexagonal, convenções idiomáticas de Kotlin 2.x, Ktor 3.x, Structured Concurrency, transações no Exposed, segurança criptográfica e isolamento estrito de banco de dados.

---

## ⚡ Diretrizes Obrigatórias de Compatibilidade Ktor 3.x

Todos os Agentes DEVEM reconhecer que este projeto utiliza **Ktor 3.x (com Kotlin 2.x)** e aplicar rigorosamente estas regras:

1. **Configuração do Servidor:**
   - O arquivo principal de configuração é [`src/main/resources/application.conf`](./src/main/resources/application.conf) (HOCON nativo do Ktor 3 com `${?VAR}`). NUNCA utilize `application.yaml` a menos que o plugin `ktor-server-config-yaml` esteja explicitamente adicionado ao Gradle.
2. **Injeção de Dependências Koin em Rotas:**
   - No Ktor 3, a classe interna `io.ktor.server.routing.RoutingKt` foi removida. O método legado `Route.inject<T>()` do `koin-ktor` falha com `NoClassDefFoundError`.
   - **Regra:** Sempre use a função desacoplada `val useCase by koinInject<T>()` definida em `DependencyInjection.kt` (que consome `GlobalContext`).
3. **Serialização Estrita de Respostas HTTP:**
   - O `kotlinx.serialization` no Ktor 3 rejeita mapas heterogêneos (`mapOf("chave" to "texto", "numero" to 123)`).
   - **Regra:** Todas as respostas em `call.respond()` DEVEM utilizar `data class` anotadas com `@Serializable` (ex: `MessageResponse`, DTOs específicos). NUNCA envie `mapOf<String, Any>`.
4. **Imports de Routing:**
   - Em todas as extension functions de rotas (`Route.xxxRoutes()`), sempre importe explicitamente `import io.ktor.server.routing.*`.

---

## 🚨 Regra Crítica de Segurança: Isolamento de Banco de Dados em Testes

- **PROIBIÇÃO ABSOLUTA**: **NUNCA** execute testes unitários ou comandos automatizados que alterem, limpem (`TRUNCATE`) ou executem drops de tabela no banco de dados principal de desenvolvimento ou produção.
- Utilize **Mocks com MockK** para isolar camadas ou utilize banco em memória (**H2**) / **Testcontainers** para testes de integração.

