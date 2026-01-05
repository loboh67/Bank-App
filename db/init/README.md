# Postgres init scripts

Files in this folder are mounted into the Postgres container at `docker-entrypoint-initdb.d`.

The official Postgres image runs these scripts **only on the first startup** of a brand new data directory (i.e., when the `postgres_data` volume is empty).

Typical workflow:

1. Export your current database schema/data to a `.sql` file (see root instructions).
2. Drop the exported file here, for example: `db/init/01_dump.sql`.
3. Recreate the DB volume and start Postgres:
   - `docker compose down -v`
   - `docker compose up -d postgres`

