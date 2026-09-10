-- ==========================================================
-- InvestEU — creates one database per microservice.
-- Runs automatically ONLY the first time the Postgres data volume is
-- empty (docker-entrypoint-initdb.d convention). If you need to re-run
-- it, remove the "pgdata" volume first: docker compose down -v
-- ==========================================================

CREATE DATABASE "usersInvestEU";
CREATE DATABASE "sectorsInvestEU";
CREATE DATABASE "projectsInvestEU";
CREATE DATABASE "documentsInvestEU";
CREATE DATABASE "interestsInvestEU";
CREATE DATABASE "messagesInvestEU";

-- "notifications" has no database of its own: it is a stateless
-- microservice (see the Block 4 documentation), so no CREATE DATABASE
-- for it here.
