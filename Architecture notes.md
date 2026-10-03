# Architecture Notes — Shared Shopping List

## Architecture goal

Keep the architecture intentionally small and understandable while still demonstrating a realistic Android + backend + CI/CD delivery loop.

## High-level structure

```text
Android app
  -> HTTP API client
  -> FastAPI backend
  -> SQLite database
```

Development and deployment split:

```text
MacBook Pro M1
  -> Android Studio / emulator or device
  -> local backend via Docker Compose during development
  -> GitHub repository and pull requests

GitHub Actions
  -> backend tests and Docker build
  -> Android unit tests and APK build
  -> optional deploy trigger

VPS
  -> isolated Docker Compose app
  -> FastAPI backend
  -> SQLite data volume
  -> health check / smoke check
```

Suggested repository layout for the implementation repository:

```text
shared-shopping-list/
  backend/
    app/
    tests/
    Dockerfile
    pyproject.toml
  android/
    app/
    build.gradle.kts
    settings.gradle.kts
  .github/
    workflows/
      backend.yml
      android.yml
      release.yml
  docker-compose.yml
  README.md
```

The implementation can live in a separate GitHub repository once the user decides to start building.

## Backend design

### Framework

Use FastAPI because it is fast to implement, easy to test, and produces OpenAPI documentation automatically.

### Persistence

Use SQLite for the MVP.

Reasoning:

- no external database service needed;
- easy local setup;
- enough for a demo;
- works well with Docker Compose;
- simple to reset during tests.

Use SQLModel for the first version to keep the backend concise and easy to test. Move to direct SQLAlchemy or PostgreSQL only if the MVP needs more explicit control later.

### API resources

- `ShoppingList`
- `ShoppingItem`

### API behavior principles

- Return predictable JSON.
- Use proper status codes.
- Return `404` for unknown list or item.
- Validate empty names.
- Keep update operations explicit.

## Android design

### UI

Use Jetpack Compose.

MVP screens:

1. Lists screen.
2. Shopping list details screen.
3. Add/edit item dialog or screen.

### State management

Keep it simple:

- Repository wraps API client.
- ViewModel exposes screen state.
- UI renders loading / content / empty / error states.

Avoid complex architecture patterns unless needed.

## Local development modes

### Backend local mode

- Run backend directly with Python.
- Run backend through Docker.

### Android local mode

- Android emulator points to local backend.
- Use environment/config for backend base URL.
- For Android emulator, local host machine backend is usually available as `http://10.0.2.2:<port>`.
- For a physical Android device, use the Mac's local network IP or the VPS backend URL.

## VPS deployment design

The backend may be hosted on the same VPS as Hermes, but it must remain operationally separate.

Recommended constraints:

- separate app directory, for example `/home/hermes/apps/shared-shopping-list/`;
- separate Docker Compose project name;
- no changes to Hermes gateway/system services;
- no committed `.env` files;
- mounted SQLite data path;
- explicit health endpoint;
- deploy script that performs pull/build/restart/smoke check;
- simple rollback path: restart previous container image or redeploy previous git revision.

Minimum smoke check after deploy:

```text
GET /health -> 200 OK
GET /lists -> 200 OK with JSON array
```

Public endpoint decision is still open. Prefer HTTPS on a subdomain once DNS/reverse proxy is ready. A temporary port or local-only demo is acceptable for the first working slice.

## Testing strategy

### Backend tests

Use pytest and FastAPI test client.

Core cases:

- health endpoint returns OK;
- create shopping list;
- get list with items;
- add item;
- toggle item as bought;
- rename item;
- delete item;
- unknown list returns 404;
- empty item name is rejected.

### Android tests

Use unit tests first.

Core cases:

- ViewModel loads list and emits content state;
- ViewModel handles API error and emits error state;
- ViewModel toggles item bought state;
- API DTOs map to UI models correctly.

Optional later:

- simple Compose UI test;
- mocked API integration test;
- generated OpenAPI client or schema check.

## CI/CD design

### Backend workflow

Run on pull request and main branch push:

- install Python dependencies;
- run lint / formatting check;
- run pytest;
- build Docker image.

### Deployment workflow

Do not automate production deployment before the MVP is stable. Start with a manual deploy script, then optionally wire it to GitHub Actions.

Manual deployment should verify:

- repository revision being deployed;
- Docker image builds;
- container starts;
- `/health` returns OK;
- logs do not show startup errors.

### Android workflow

Run on pull request and main branch push:

- set up JDK;
- run Gradle unit tests;
- run Android build;
- upload APK artifact.

### Release workflow

Optional tag-based workflow:

- build backend Docker image;
- build Android APK;
- upload release artifacts.

## API contract

FastAPI should generate OpenAPI automatically.

Possible contract checks:

- export `openapi.json` in CI;
- store it as a CI artifact;
- later compare schema changes if the Android client depends on stable fields.

## Scope control

Do not add these in MVP:

- login;
- permissions;
- real-time WebSocket sync;
- push notifications;
- barcode scanning;
- cloud deployment;
- offline-first sync;
- complex dependency injection setup.

The first version should be demoable, tested, and easy to explain.
