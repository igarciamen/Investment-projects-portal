# InvestEU — local deployment with docker compose

This closes Block 7 (Week 5) of the roadmap: every backend microservice
(`users`, `sectors`, `projects`, `documents`, `notifications`, `interests`,
`messages`) plus Postgres, orchestrated together with a single command.

## Layout

Each `Dockerfile` below is identical (a generic two-stage Maven build), and
must sit at the root of its own module, alongside that module's `pom.xml`:

```
InvestEU/
├── docker-compose.yml
├── init-databases.sql
├── .env.example
├── .env                  <- you create this, never committed
├── users/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── sectors/
│   ├── Dockerfile
│   └── ...
├── projects/
├── documents/
├── notifications/
├── interests/
└── messages/
```

If your modules currently live in separate folders with different names,
adjust the `build:` paths in `docker-compose.yml` to match.

## Setup

1. Copy the environment template and fill in your real values:
   ```
   cp .env.example .env
   ```
   Edit `.env`: at minimum, set `GMAIL_USERNAME` and `GMAIL_APP_PASSWORD` to
   your own Gmail app password (see the Block 4 documentation) if you want
   `notifications` to send real emails. The other defaults
   (`JWT_SECRET`, `DB_PASSWORD`, `ADMIN_USERNAME`/`ADMIN_PASSWORD`) already
   match what every microservice's `application.properties` expects, so you
   can leave them as-is for local/portfolio use.

2. Build and start everything:
   ```
   docker compose up --build
   ```
   First run downloads base images and builds all 7 jars — expect a few
   minutes. Subsequent runs are much faster (Docker layer caching).

3. Once every container is up, the same ports you already used for local
   (non-Docker) testing are exposed on your machine:

| Service | Port | Swagger UI |
|---|---|---|
| users | 8081 | http://localhost:8081/swagger-ui.html |
| sectors | 8082 | http://localhost:8082/swagger-ui.html |
| projects | 8083 | http://localhost:8083/swagger-ui.html |
| documents | 8084 | http://localhost:8084/swagger-ui.html |
| notifications | 8085 | http://localhost:8085/swagger-ui.html |
| interests | 8086 | http://localhost:8086/swagger-ui.html |
| messages | 8087 | http://localhost:8087/swagger-ui.html |
| postgres | 5432 | (no UI — connect with any Postgres client if needed) |

Every Swagger walkthrough from the Block 1-6 documentation works exactly the
same against these containerized services as it did running them from
IntelliJ — the only thing that changed under the hood is how each service
finds the others (container names instead of `localhost`, see below).

## How cross-service calls work inside Docker

Every `application.properties` still says `http://localhost:808X/...` for
the other microservices — that value is only ever used when you run a
service directly from IntelliJ/Maven on your own machine. Inside Docker,
`docker-compose.yml` overrides those same properties with environment
variables (Spring Boot's relaxed binding maps `SECTORS_BASE_URL` to
`sectors.base-url` automatically, no code change needed), pointing at the
other containers by their Compose service name instead:

```yaml
SECTORS_BASE_URL: http://sectors:8082/api/sectors
```

This is why no `SecurityConfig`, `*Client.java`, or `application.properties`
needed to change for this block — only new infrastructure files were added.

## Resetting the database

`init-databases.sql` only runs the first time Postgres starts with an empty
data volume. To start over from a clean database:

```
docker compose down -v
docker compose up --build
```

(`-v` removes the named volumes, including `pgdata` and `documents-uploads`
— you will lose all data and uploaded files.)

## Stopping everything

```
docker compose down
```

Data survives (the `pgdata` and `documents-uploads` volumes are not
removed) — the next `docker compose up` picks up right where you left off.
