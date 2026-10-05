# Backend — Shared Shopping List

FastAPI + SQLModel + SQLite backend exposing the list/item CRUD API and
`GET /health`.

## Containerized only (AC41)

The backend is built, run and tested **exclusively through Docker / Docker
Compose**. The host needs only **Docker Desktop** — there is **no host Python
toolchain** and no native fallback. All commands below are run from the
repository root.

The `backend` and `backend-test` Compose services share one image
(`backend/Dockerfile`, `python:3.11-slim`) built from `backend/`.

## Configuration

- The backend reads a single variable, `DATABASE_URL`, from the root `.env`
  file (`docker compose` loads it automatically). Copy the template:
  `cp .env.example .env`.
- `.env.example` contains **no secrets**; the real `.env` is git-ignored and is
  never committed (`.gitignore`, AC16 / MP-15).
- The default database is `sqlite:///./data/app.db`. The `backend` service
  bind-mounts `./data` to `/app/data`, so the SQLite file persists on the host
  across restarts and rebuilds.

## Run the backend

```sh
docker compose up backend          # foreground; Ctrl-C to stop
docker compose up -d backend       # detached
docker compose ps                  # shows the backend as healthy
docker compose down                # stop and remove the container
```

The API is exposed on <http://localhost:8000>:

```sh
curl -s localhost:8000/health                 # -> {"status":"ok"}
curl -s localhost:8000/lists                  # -> [] (or the stored lists)
curl -s -X POST localhost:8000/lists \
  -H 'content-type: application/json' \
  -d '{"name":"Weekend groceries"}'           # -> 201 ShoppingList
```

A Compose `healthcheck` polls `GET /health` every 10 s, so
`docker compose ps` reports the container as `healthy` once the app is up
(AC14/AC15).

## Lint and tests (in the container)

```sh
# Full gate (ruff check + ruff format --check + pytest) — this is the
# `backend-test` service default command:
docker compose run --rm backend-test

# Just the test suite:
docker compose run --rm backend-test pytest

# Just the linters:
docker compose run --rm backend-test sh -c "ruff check . && ruff format --check ."
```

`backend-test` requires no running `backend` container and never touches the
host Python installation (AC41 / HP-15).

## CI note — OQ-5 decision

**Decision (applied in Stage 3):** the backend CI Docker job **builds the image
and also runs the container and hits `GET /health`**, asserting both HTTP `200`
and the body `{"status":"ok"}` — rather than a build-only job. This makes
AC15's runtime health check CI-covered in addition to the local Compose check
above, so a broken runtime/startup fails the pull request instead of only the
local/manual runbook.
