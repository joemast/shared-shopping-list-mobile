# AGENTS.md — Shared Shopping List

## What this repo is

Implemented MVP: Kotlin/Compose Android client + FastAPI/SQLModel backend + Docker/Compose
local loop + GitHub Actions + a manual isolated VPS deploy. It is no longer a planning-only
repo, despite what older notes may say.

Docs map:
- `README.md` — authoritative architecture, API contract, testing/deploy overview.
- `deploy/README.md` — the full VPS runbook (isolation, smoke check, rollback).
- `backend/README.md` / `android/README.md` — component-specific decisions and pins.
- `my-specs/docs/*.md` — planning docs (tracked).
- `Dashboard.md` — **stale**: it still claims no implementation exists. Trust code/CI/README.

## Container-first: no host toolchain

Only Docker Desktop is required. There is intentionally **no host Python, JDK, Android SDK, or
Gradle**, and no native fallback. Run every command below from the repo root.

Backend:

```sh
docker compose up -d backend                 # API on :8000; SQLite in ./data
docker compose run --rm backend-test         # default gate: ruff check + format --check + pytest
docker compose run --rm backend-test pytest  # tests only
docker compose run --rm backend-test pytest tests/test_items.py -k update   # focused
docker compose run --rm backend-test sh -c "ruff check . && ruff format --check ."
```

Android:

```sh
docker compose build android-build           # first build is multi-GB and slow
docker compose run --rm android-build ./gradlew testDebugUnitTest assembleDebug
docker compose run --rm android-build ./gradlew testDebugUnitTest --tests "com.example.sharedshoppinglist.viewmodel.ListsViewModelTest"
docker compose run --rm android-build ./gradlew lintDebug
docker compose run --rm android-build ./gradlew assembleDebugAndroidTest   # COMPILE UI tests only
```

`connectedDebugAndroidTest` (the instrumented UI suite) runs **only in CI** on an Ubuntu + KVM
emulator. Docker Desktop on macOS has no KVM, so a local run is unsupported — not a skipped pass.
Locally you only compile `androidTest` sources.

## Gotchas that are easy to miss

- Emulator reaches the host backend at **`10.0.2.2:8000`, never `localhost`**.
  `BaseUrlConfigTest` asserts `BuildConfig.API_BASE_URL == "http://10.0.2.2:8000/"`; if you
  repoint the debug build (e.g. a LAN IP for a physical device), update that test too.
- API JSON is **`snake_case`**; Android DTOs use `@SerialName`. PATCH partial updates depend on
  `ApiClient.json { explicitNulls = false }` on Android and `model_dump(exclude_unset=True)` on
  the backend. Keep both in sync.
- Backend tests use a **file-backed** temp SQLite DB (not `:memory:`) so persistence tests are
  real. `create_app(settings)` builds a fresh engine per instance — tests rely on this.
- Router order matters: `lists` is registered before `items` in `app/main.py` so
  `/lists/{id}` does not shadow `/lists/{id}/items`.
- **No DI framework.** `AppContainer` is a framework-free service locator with
  `installTestOverride(...)`. JVM tests use hand-written `FakeShoppingRepository`; instrumented
  UI tests use MockWebServer. Do not introduce Hilt/Koin/Dagger.
- Don't `git add` local-only files: `my-specs/tasks/`, `.opencode/`, and `opencode.jsonc` are
  git-ignored (only the four `my-specs/docs/*.md` are tracked, from before the ignore rule).
  Never commit `.env`; `.env.example` holds no secrets (`DATABASE_URL` only).
- Apple Silicon: `android-build` runs as `linux/amd64` because Google ships **no linux/aarch64
  Android SDK** — enable Rosetta 2 for Docker. A named `gradle-cache` volume persists the Gradle
  home between `docker compose run` invocations; memory is pinned low in `gradle.properties`
  (~7.7 GiB Docker VM) so raising parallelism can OOM the build.
- Deployment is **manual only** — there is no CI deploy. `deploy/deploy.sh` is for the isolated
  VPS app dir with Compose project `-p shared-shopping-list`; it must not touch Hermes.

## Pinned versions (don't drift casually)

- Android: Kotlin 1.9.24, Compose Compiler 1.5.14 (pinned pair), Compose BOM 2024.06.00,
  AGP 8.5.2, Gradle 8.7, JDK 17, min/target/compileSdk 24/34/34, Retrofit 2.11.0,
  kotlinx.serialization 1.6.3.
- Backend: Python 3.11, FastAPI 0.115.5, SQLModel 0.0.22, pydantic 2.10.3, ruff 0.8.4
  (line length 100; lint rule set `E,F,I,B,UP`), pytest 8.3.4.

## CI (GitHub Actions)

Both workflows run on every PR and on pushes to `main`, one per stack:
- `backend.yml` — native ruff + pytest, export OpenAPI artifact, `docker build`, then run the
  image and assert `GET /health` → `200 {"status":"ok"}`.
- `android.yml` — fast `unit` job (`testDebugUnitTest assembleDebug` + `demo-apk` artifact) and
  a merge-blocking `ui-tests` job (`connectedDebugAndroidTest` on the pinned API 34 emulator).

## Frozen scope (do not re-litigate)

Sharing is a list-ID/link concept — no accounts. Do not add auth/user management, real-time
sync/WebSockets, push notifications, offline sync, barcode/recipes, cloud deploy, or heavy
DI/architecture frameworks. The SQLite schema is frozen for the MVP (no Alembic).

## Shift-Left Testing workflow

This repo uses the `/slt-*` command workflow from `.opencode/commands/`. If asked to continue
the project workflow, use `/slt` and respect its stage/checklist gates. Task artifacts live in
`my-specs/tasks/<short-name>/` (local-only); the active task is `01-shared-shopping-list-mvp`,
currently in stage 04 (`04-tasks-progress.md`). Do not skip stages silently.
