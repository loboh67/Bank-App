# Docker (Postgres + services)

## 1) Start only the database

Create a `.env` file (or export env vars) based on `.env.example`, then:

```bash
docker compose up -d postgres
```

This creates a Postgres database named `auth_service` (by default) and exposes it on `localhost:5433` (default).

## 2) Connect with DBeaver

Create a PostgreSQL connection with:

- Host: `localhost`
- Port: `5433` (or `${POSTGRES_PORT}`)
- Database: `auth_service` (or `${POSTGRES_DB}`)
- User: `henriquelobo` (or `${POSTGRES_USER}`)
- Password: `postgres` (or `${POSTGRES_PASSWORD}`)

## 3) Bring your existing schema/data into the container

### Option A — DBeaver export (recommended if you already use it)

In DBeaver:

1. Right-click your database (or schema) → **Tools** → **Backup** (or **Dump database**).
2. Choose **Format: Plain** (SQL).
3. Select **Schema** + **Data** (or only schema if you want an empty DB).
4. Save the file as `db/init/01_dump.sql`.
5. Recreate the Postgres volume and start again (init scripts only run on first boot):

```bash
docker compose down -v
docker compose up -d postgres
```

### Option B — `pg_dump` / `psql` (CLI)

Export from your current local DB:

```bash
pg_dump -h localhost -p 5432 -U <your_user> -d auth_service -F p > db/init/01_dump.sql
```

Then recreate the volume as shown above.

If you want to import into an *already running* container (without recreating the volume), run:

```bash
docker exec -i lobo-postgres psql -U "${POSTGRES_USER:-henriquelobo}" -d "${POSTGRES_DB:-auth_service}" < db/init/01_dump.sql
```

## 4) Run the core services (optional)

The repo includes Dockerfiles for the Java services. This compose file wires `auth`, `enable-banking`, and `api` to Postgres via the hostname `postgres`.

```bash
docker compose --profile core up -d --build
```

Notes:

- `api` defaults to `JPA_DDL_AUTO=validate` (in `api/src/main/resources/application.yaml`), so the schema must exist.
- `auth` and `enable-banking` use `ddl-auto=update` by default and can create/update tables, but relying on this long-term is risky; consider migrations once everything is stable.
