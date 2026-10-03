# Implementation Notes — Shared Shopping List

## Recommended repository name

Preferred: `shared-shopping-list`

Alternatives:

- `shopping-list-demo`
- `android-backend-shopping-list`
- `shared-groceries-demo`

## Recommended stack

### Backend

- Python 3.11+
- FastAPI
- SQLite
- SQLModel for the MVP
- Alembic can be skipped in the first version; add it only if schema changes become painful
- pytest
- Docker
- Docker Compose for local and VPS deployment

### Android

- Kotlin
- Jetpack Compose
- Retrofit for REST API client
- kotlinx.serialization or Moshi for JSON mapping
- ViewModel
- Gradle
- Android Studio on MacBook Pro M1 for local development and emulator/device testing

### CI/CD

- GitHub Actions
- Backend test workflow
- Android build/test workflow
- Docker build workflow
- APK artifact upload
- Deploy workflow or deploy script for the VPS after the MVP works

## Development environment

Primary development machine: Alexander's MacBook Pro M1.

Expected local tools:

- Android Studio.
- JDK compatible with the Android Gradle Plugin.
- Android SDK and emulator, or a physical Android device.
- Python 3.11+.
- Docker Desktop or another Docker runtime.
- GitHub CLI optional but useful.

The AI coding agent can work through the Git repository and CLI commands. Android Studio is still useful for emulator/device checks, visual debugging, and inspecting Gradle/Android project state.

## Backend hosting decision

Use a custom backend hosted on the same VPS that runs Hermes, but keep it isolated from Hermes.

Hosting rules:

- Use a separate directory, for example `/home/hermes/apps/shared-shopping-list/`.
- Use Docker Compose.
- Do not reuse Hermes gateway ports or service files.
- Expose only the API port or reverse-proxied HTTPS endpoint.
- Keep logs, data volume, and environment file separate.
- Add `/health` endpoint and a smoke-check command.
- Keep secrets out of git. The MVP should avoid secrets where possible.

Recommended MVP deployment shape:

```text
VPS
  /home/hermes/apps/shared-shopping-list/
    docker-compose.yml
    .env              # not committed
    data/             # SQLite database volume/path
    logs/             # optional logs

  shared-shopping-list-backend container
    -> FastAPI app
    -> SQLite database file in mounted volume
```

Public access options:

1. Preferred later: HTTPS subdomain, for example `https://shopping-api.<domain>/`.
2. MVP fallback: temporary public port with firewall/reverse-proxy decision documented.
3. Local-only demo fallback: run backend locally during Android demo.

## MVP API sketch

```text
GET /health

POST /lists
GET /lists
GET /lists/{list_id}

POST /lists/{list_id}/items
PATCH /items/{item_id}
DELETE /items/{item_id}
```

Recommended JSON shapes:

```json
{
  "id": "uuid",
  "name": "Weekend groceries",
  "created_at": "2026-10-01T12:00:00Z",
  "updated_at": "2026-10-01T12:00:00Z",
  "items": []
}
```

```json
{
  "id": "uuid",
  "list_id": "uuid",
  "name": "milk",
  "quantity": "2 bottles",
  "bought": false,
  "created_at": "2026-10-01T12:00:00Z",
  "updated_at": "2026-10-01T12:00:00Z"
}
```

## MVP backend test checklist

- Health endpoint returns OK.
- Can create a list.
- Can get all lists.
- Can get one list with items.
- Can add item.
- Can toggle item bought state.
- Can rename item.
- Can delete item.
- Unknown list returns 404.
- Empty item name is rejected.

## MVP Android test checklist

- ViewModel emits loading then content state.
- ViewModel emits empty state for empty list.
- ViewModel emits error state on API failure.
- Toggle bought updates UI model.
- DTO-to-domain mapping works.

## Demo script

1. Show README and architecture.
2. Show GitHub Actions checks.
3. Start backend locally.
4. Open Android app.
5. Create `Weekend groceries`.
6. Add `milk`, `bread`, and `apples`.
7. Mark `milk` as bought.
8. Refresh/restart app and show persisted state.
9. Explain tests and CI quality gates.
10. Explain non-goals and next improvements.

## First implementation slice

Build in this order:

1. Backend `/health` endpoint with pytest.
2. Backend `ShoppingList` model and `POST /lists`, `GET /lists` tests.
3. Backend `ShoppingItem` model and item CRUD tests.
4. Backend Dockerfile and Docker Compose local run.
5. Backend GitHub Actions workflow.
6. Android project skeleton with one screen and fake/static state.
7. Retrofit client and repository wired to backend.
8. ViewModel tests for loading and toggling item state.
9. Android GitHub Actions build/test workflow.
10. VPS deploy script and smoke check.

Do not start Android UI polish before backend API and CI checks are stable.

## Scope guardrails

Do not start with:

- authentication;
- real-time collaboration;
- cloud deployment;
- offline sync;
- barcode scanning;
- recipe/grocery catalog features;
- polished design system.

The goal is to finish a small, working, tested, explainable project.
