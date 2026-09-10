# Investment Projects Platform (Promoter / Investor)

## Demo
https://github.com/user-attachments/assets/e9ecf8a1-2c3e-428e-bc34-61a87e899688

A matchmaking platform connecting investment project promoters with potential
investors, built as a functional and visual replica of the European
Commission's real **[InvestEU Portal](https://europa.eu/investeu/investeu-portal_en)**
(EIPP). A promoter submits a project, an admin evaluates and approves or
rejects it, and once approved, investors can discover it in a public
catalog, express interest, and contact the promoter directly.

True to the real portal, **no financial transaction ever happens inside this
platform**. Closing an investment (transfer, contract, due diligence) is
out of scope, exactly as it is on the real InvestEU Portal, the
system is a showcase and a point of contact, nothing more.


## Architecture

Seven independent backend microservices, each with its own PostgreSQL
database (except `notifications`, which is stateless), plus an Angular
frontend that talks to them directly. No API Gateway, a deliberate choice to
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
another's authorization decision, it asks and forwards the caller's own
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
`localhost`, via environment variables, no code changes needed) and how to
reset the database.

## Running individual services locally

Each backend module has its own `pom.xml` and can be run independently from
your IDE (`mvn spring-boot:run`, or the IDE's own run configuration) against
a local PostgreSQL instance. The frontend runs with the standard Angular CLI:

```bash
cd frontend
ng serve
```
