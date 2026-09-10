# projects microservice — Block 1 (Week 1)

Investment platform project (Promoter / Investor). This module is a **new**,
sibling Maven module to `users` — not nested inside it.

Scope of this block: basic CRUD for `Project`, created in `DRAFT` status.
Status transitions (`submit` / `review` / `approve` / `reject`) and the
`SectorClient` integration with the future `sectors` microservice are
intentionally left as a `TODO` — they are built in Block 2, once `sectors`
exists.

## Running it

1. Start Postgres and make sure the `users` microservice is running on port
   `8081` (this service validates the JWT tokens `users` issues, using the
   **same** `jwt.secret`).
2. Run this service — it listens on port `8083` and will create the
   `projectsInvestEU` database on first boot (`ddl-auto=update`).
3. Open `http://localhost:8083/swagger-ui.html`.

## Testing in Swagger

1. On `users` (`http://localhost:8081/swagger-ui.html`), register a promoter:

```json
POST /api/auth/signup
{
  "username": "ana",
  "email": "ana@promoter.com",
  "password": "123456",
  "userType": "PROMOTER"
}
```

2. Log in and copy the `token`:

```json
POST /api/auth/login
{
  "login": "ana",
  "password": "123456"
}
```

3. On `projects` Swagger UI, click **Authorize** and enter `Bearer <ana's token>`.

4. Create a project:

```json
POST /api/projects
{
  "sectorId": 1,
  "title": "Solar plant in Extremadura",
  "description": "5MW photovoltaic installation for industrial self-consumption",
  "country": "Spain",
  "requestedAmount": 250000.00
}
```
→ `201 Created`, `status: "DRAFT"`.

5. `GET /api/projects/mine` → should return the project you just created.
6. `GET /api/projects/{id}` (with the returned id) → detail view.
7. `PUT /api/projects/{id}` with new data → edits it (works because it is
   still `DRAFT`):

```json
PUT /api/projects/{id}
{
  "sectorId": 2,
  "title": "Solar plant in Extremadura (updated)",
  "description": "6MW photovoltaic installation",
  "country": "Spain",
  "requestedAmount": 300000.00
}
```

8. **Role-rejection check**: register a second user with
   `"userType": "INVESTOR"`, log in, authorize with their token, and try
   `POST /api/projects` → must return **403 Forbidden**.

9. **Admin check**: on `users`, the `isabel` / `123456` account (seeded by
   `DataLoader`) already has `ROLE_ADMIN`. Log in with that user, authorize in
   the `projects` Swagger UI with their token, and try
   `GET /api/projects/all` → should list projects from every promoter.

Note: role literals such as `ROLE_PROMOTER`, `ROLE_INVESTOR`, `ROLE_ADMIN`,
and the `userType` values `PROMOTER` / `INVESTOR` are a **shared contract**
between `users` and `projects` (and every future microservice): they must be
spelled identically on both sides, since `projects` validates JWTs issued by
`users` without ever calling it. If you ever rename a role, it has to change
in `users` (`ERole`, `DataLoader`, `SignupRequest`) and in every consuming
service's `SecurityConfig`/tests at the same time.

## Next step

Block 2 (Week 1-2): the `sectors` microservice, plus the status-transition
endpoints in `ProjectService`, wiring in the `SectorClient` that is currently
left as a `TODO`.
