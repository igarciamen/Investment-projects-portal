# Invertment Proyect — Investment Projects Platform (Promoter / Investor)

A matchmaking platform connecting investment project promoters with potential
investors, built as a functional and visual replica of the European
Commission's real **[InvestEU Portal](https://europa.eu/investeu/investeu-portal_en)**
(EIPP). A promoter submits a project, an admin evaluates and approves or
rejects it, and — once approved — investors can discover it in a public
catalog, express interest, and contact the promoter directly.

True to the real portal, **no financial transaction ever happens inside this
platform**. Closing an investment (transfer, contract, due diligence) is
explicitly out of scope, exactly as it is on the real InvestEU Portal — the
system is a showcase and a point of contact, nothing more.

This is a portfolio / TFG (Bachelor's thesis) project built with a
microservices backend in Spring Boot and an Angular frontend.

---

## Demo


https://github.com/user-attachments/assets/320ab818-9aaf-4507-bc9e-a3d9b0079649

---

## Table of contents

- [Demo](#demo)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Quick start (Docker Compose)](#quick-start-docker-compose)
- [Running individual services locally](#running-individual-services-locally)
- [Project structure](#project-structure)
- [Role contract](#role-contract)
- [Testing](#testing)
- [Documentation](#documentation)
- [Roadmap / status](#roadmap--status)

---

## Architecture

Seven independent backend microservices, each with its own PostgreSQL
database (except `notifications`, which is stateless), plus an Angular
frontend that talks to them directly — no API Gateway, a deliberate choice to
keep infrastructure complexity within TFG scope.

```
                        ┌─────────────┐
                        │  frontend   │  Angular, port 4200
                        └──────┬──────┘
                               │
        ┌──────────┬──────────┼──────────┬──────────┬──────────┐
        ▼          ▼          ▼          ▼          ▼          ▼
    ┌───────┐  ┌───────┐  ┌────────┐ ┌─────────┐ ┌─────────┐ ┌────────┐
    │ users │  │sectors│  │projects│ │documents│ │interests│ │messages│
    │ 8081  │  │ 8082  │  │  8083  │ │  8084   │ │  8086   │ │  8087  │
    └───────┘  └───────┘  └───┬────┘ └────┬────┘ └────┬────┘ └───┬────┘
                               │           │           │          │
                               └───────────┴─────┬─────┴──────────┘
                                                  ▼
                                          ┌───────────────┐
                                          │ notifications │  8085
                                          │  (stateless)  │
                                          └───────────────┘
```

`projects` is the hub of the domain: `sectors`, `documents`, `interests`, and
`messages` all call it to resolve ownership/access, and it in turn calls
`users`, `sectors`, and `notifications`. No microservice ever duplicates
another's authorization decision — it asks and forwards the caller's own
token.

| Microservice | Port | Responsibility |
|---|---|---|
| `users` | 8081 | Authentication (JWT), roles, user profile |
| `sectors` | 8082 | Investment sector catalog, with soft-delete |
| `projects` | 8083 | Core domain: project CRUD, status workflow, public catalog |
| `documents` | 8084 | Project attachments (business plan, accounts, technical report) |
| `notifications` | 8085 | Generic email sending via SMTP; the only stateless service |
| `interests` | 8086 | Investor expressions of interest |
| `messages` | 8087 | Direct promoter–investor contact (one thread per project+investor pair) |

## Tech stack

**Backend:** Java 21, Spring Boot, Spring Security (JWT / OAuth2 Resource
Server), Spring Data JPA, PostgreSQL, springdoc-openapi (Swagger UI).

**Backend testing:** JUnit 5, Mockito, ArchUnit, H2 (in-memory, for
integration tests), MockMvc.

**Frontend:** Angular (standalone components), RxJS, Angular Reactive Forms,
Bootstrap 5.

**Infrastructure:** Docker, Docker Compose, PostgreSQL 16.

**Notifications:** SMTP via Gmail, HTML template rendered with Thymeleaf.

## Quick start (Docker Compose)

```bash
cp .env.example .env
# edit .env — at minimum set GMAIL_USERNAME / GMAIL_APP_PASSWORD
# if you want real emails; the rest of the defaults already work locally

docker compose up --build
```

First run takes a few minutes (base images + 7 jars compiling). Once every
container is up:

| Service | Swagger UI |
|---|---|
| users | http://localhost:8081/swagger-ui.html |
| sectors | http://localhost:8082/swagger-ui.html |
| projects | http://localhost:8083/swagger-ui.html |
| documents | http://localhost:8084/swagger-ui.html |
| notifications | http://localhost:8085/swagger-ui.html |
| interests | http://localhost:8086/swagger-ui.html |
| messages | http://localhost:8087/swagger-ui.html |
| frontend | http://localhost:4200 |

See `README-deployment.md` for the full walkthrough, including how
cross-service calls resolve inside Docker (container names instead of
`localhost`, via environment variables — no code changes needed) and how to
reset the database.

## Running individual services locally

Each backend module has its own `pom.xml` and can be run independently from
your IDE (`mvn spring-boot:run`, or the IDE's own run configuration) against
a local PostgreSQL instance. The frontend runs with the standard Angular CLI:

```bash
cd frontend
ng serve
```

## Project structure

```
Invertment Proyect/
├── docker-compose.yml
├── init-databases.sql
├── .env.example
├── README-deployment.md
├── users/          # pom.xml, Dockerfile, src/
├── sectors/
├── projects/
├── documents/
├── notifications/
├── interests/
├── messages/
└── frontend/       # Angular app
```

Each backend module follows the same internal layout:
`model` / `repository` / `service` / `controller` / `payloads` (request +
response) / `config` (security, OpenAPI, and — where applicable —
`RestTemplateConfig` for calling other microservices).

## Role contract

`ROLE_ADMIN`, `ROLE_PROMOTER`, and `ROLE_INVESTOR` (plus the signup values
`PROMOTER` / `INVESTOR`) are a shared contract across every microservice.
Each service validates the JWT that `users` issues on its own, without
calling `users` on every request — so all of them must agree on the exact
same role spelling. If you ever rename a role, update it everywhere at once:
`users` (`ERole`, `DataLoader`, `SignupRequest`, tests) and every consuming
service's `SecurityConfig` and tests.

## Testing

Every backend microservice with its own logic follows the same three-tier
pattern:

1. **`ArchitectureTest`** (ArchUnit) — structural rules: controllers end in
   `Controller`, live in the right package, and never depend directly on a
   repository.
2. **Unit tests** (Mockito) — each service's business logic, with its
   dependencies (repositories, HTTP clients) mocked.
3. **Integration tests** (`@SpringBootTest` + MockMvc + in-memory H2) — the
   full HTTP flow, with simulated real JWTs and any client toward another
   microservice mocked with `@MockitoBean`.

Run a module's tests from its own folder:

```bash
cd projects
mvn test
```

## Documentation

Full technical documentation — architecture, data model, endpoints,
design decisions, manual testing walkthroughs, and real issues found and
fixed during development — is split by block in `docs/` (or wherever these
`.docx` files are kept in your copy of the repo):

| Block | Document |
|---|---|
| Backend 1 | `Microservicio_projects_Bloque1.docx` |
| Backend 2 | `Microservicio_sectors_y_flujo_estados_Bloque2.docx` |
| Backend 3 | `Microservicio_documents_Bloque3.docx` |
| Backend 4 | `Microservicio_notifications_Bloque4_v2.docx` |
| Backend 5 | `Portal_del_inversor_Bloque5.docx` |
| Backend 6 | `Panel_admin_Bloque6.docx` |
| Backend 7 | `README-deployment.md` (Docker Compose, `.env`) |
| Frontend | `Frontend_Portal_del_inversor.docx` |
| Frontend | `Frontend_Documentos_y_contacto.docx` |
| Frontend | `Notificaciones_messages_documents.docx` |
| Frontend | `Frontend_Indicador_no_leidos.docx` |
| Frontend | `Frontend_Terminos_y_condiciones.docx` |
| Frontend | `Frontend_My_Profile_Organisation.docx` |
| Frontend | `Frontend_My_Investments.docx` |

The full thesis memoria, integrating all of the above, is in
`Memoria_TFG_InvestEU.docx`, and the chapter-index version (following the
university's required outline) is in `Indice_Capitulos_TFG_InvestEU.docx`.

## Roadmap / status

| Block | Content | Status |
|---|---|---|
| 1 | `users` (roles) + `projects` (basic CRUD) | ✅ Done |
| 2 | `sectors` + `projects` status workflow | ✅ Done |
| 3 | `documents` | ✅ Done |
| 4 | `notifications`, wired into `projects` | ✅ Done |
| 5 | Public catalog, `interests`, `messages` | ✅ Done |
| 6 | Admin panel (pending evaluation, metrics) | ✅ Done |
| 7 | Docker Compose, `.env`, deployment | ✅ Done |
| Frontend | Full portal UI, faithful to the real InvestEU Portal | ✅ Done |

Every block above was closed only after passing both its automated test
suite and a manual end-to-end verification (Swagger and/or browser) — see the
per-block documentation for the real issues that surfaced during that
process and how each was fixed.
