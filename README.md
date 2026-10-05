# Shared Shopping List

A deliberately small **Android + backend** project that demonstrates a complete
delivery loop: mobile client, backend API, automated tests, quality gates, a
Dockerized build, an isolated deployment, and a repeatable end-to-end demo.

The product is intentionally simple — the point is the *engineering workflow*,
not the shopping domain. It is a hands-on portfolio/interview project showing
practical work across Android, backend, testing, and CI/CD.

> Planning docs live under `my-specs/` (git-ignored in this repo); the frozen
> decisions and scope guardrails are in [`AGENTS.md`](AGENTS.md) and
> [`Dashboard.md`](Dashboard.md). Everything tracked here is the implementation.

---

## Why this project exists

A generic ToDo app is easy to understand but too abstract for a useful portfolio
demo. A shared shopping list keeps the same low functional complexity while
adding a small real-world collaboration scenario: one list used by more than one
person, with items that can be added, marked as bought, renamed, or removed, and
persisted through a backend API.

It exists to demonstrate, end to end:

- Android development with **Kotlin + Jetpack Compose**;
- backend API development with **FastAPI + SQLModel + SQLite**;
- client–server integration (Retrofit → repository → ViewModel → Compose);
- automated tests on **both** sides (pytest + JVM unit tests + instrumented UI tests);
- **GitHub Actions** quality gates (backend lint/pytest/Docker; Android unit + emulator UI + APK artifact);
- a **containerized local dev/test loop** (Docker Desktop is the only host tool);
- an **isolated VPS deployment** that leaves Hermes untouched;
- a clear, repeatable demo.

---

## Architecture

```text
┌────────────────────────────┐   JSON over HTTP    ┌───────────────────────────────┐
│        ANDROID CLIENT       │  ───────────────▶   │          FASTAPI BACKEND       │
│  Kotlin + Jetpack Compose   │                     │  Python 3.11 + FastAPI         │
│  Retrofit + ViewModel       │  ◀───────────────   │  SQLModel + SQLite             │
│  Lists + Details screens    │   200/201/204/      │  /health  /lists  /items       │
│  UiState: Loading/Content/  │   404/422           │  pytest (TestClient)           │
│           Empty/Error       │                     │  OpenAPI auto-generated        │
└──────────────┬──────────────┘                     └───────────────┬───────────────┘
               │                                                    │ SQL
   emulator →  http://10.0.2.2:8000/                                 ▼
   (BuildConfig.API_BASE_URL)                        ┌───────────────────────────────┐
                                                      │  SQLite — mounted volume       │
                                                      │  (./data/app.db locally,       │
                                                      │   app-dir/data on the VPS)     │
                                                      └───────────────────────────────┘

LOCAL LOOP (Docker Desktop only — no host Python/JDK/Android SDK/Gradle)
  docker compose up backend                 → FastAPI on http://localhost:8000
  docker compose run --rm backend-test      → ruff check + ruff format --check + pytest
  docker compose run --rm android-build     → ./gradlew assembleDebug | testDebugUnitTest
                                              ./gradlew assembleDebugAndroidTest  (COMPILE only)

CI-ONLY UI EXECUTION (AC42)
  GitHub Actions · Ubuntu + KVM · ReactiveCircus/android-emulator-runner
  ./gradlew connectedDebugAndroidTest        → the ONLY place the UI suite runs

DEPLOY (manual, isolated)
  deploy/deploy.sh  →  pull · build · up -d (-p shared-shopping-list) · smoke-check · Hermes verify
```

### API contract (exact)

| Method | Path | Body | Success | Errors |
|--------|------|------|---------|--------|
| GET | `/health` | — | `200 {"status":"ok"}` | — |
| POST | `/lists` | `{"name": str}` | `201 ShoppingList` (`items: []`) | `422` |
| GET | `/lists` | — | `200 [ShoppingList, ...]` (each with `items`) | — |
| GET | `/lists/{list_id}` | — | `200 ShoppingList` (with `items`) | `404` |
| POST | `/lists/{list_id}/items` | `{"name": str, "quantity": str\|null}` | `201 ShoppingItem` | `404`, `422` |
| PATCH | `/items/{item_id}` | partial `{"name"?, "quantity"?, "bought"?}` | `200 ShoppingItem` | `404`, `422` |
| DELETE | `/items/{item_id}` | — | `204` (no content) | `404` |

All JSON is `snake_case`; timestamps are ISO-8601 UTC; IDs are UUID strings;
`quantity` is optional free text (e.g. `"2 bottles"`).

---

## Repository layout

```text
.
├── backend/                     # FastAPI + SQLModel + SQLite
│   ├── app/                     # main, config, database, models, schemas, routers/
│   ├── tests/                   # pytest (file-backed temp SQLite)
│   ├── Dockerfile               # python:3.11-slim (runtime + backend-test)
│   ├── pyproject.toml
│   ├── .env.example
│   └── README.md
├── android/                     # Kotlin + Jetpack Compose client
│   ├── app/src/main/            # MainActivity, AppContainer, domain/, data/, viewmodel/, ui/
│   ├── app/src/test/            # JVM unit tests (ViewModel + DTO mapping)
│   ├── app/src/androidTest/     # instrumented UI smoke suite (compiled locally, run in CI)
│   ├── Dockerfile               # Android toolchain image (JDK + SDK + Gradle)
│   └── README.md
├── deploy/
│   ├── deploy.sh                # manual isolated VPS deploy + Hermes verify
│   ├── smoke-check.sh           # GET /health + GET /lists assertions
│   └── README.md                # deploy steps, isolation checklist, rollback, OQ-9 commands
├── .github/workflows/
│   ├── backend.yml              # lint + pytest + Docker build + /health
│   └── android.yml              # JVM unit job + emulator UI job (CI-only) + APK artifact
├── docker-compose.yml           # backend · backend-test · android-build
├── .env.example
└── README.md
```

---

## Technology stack (locked decisions)

| Area | Choice |
|------|--------|
| Android | Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00), AGP 8.5.2, JDK 17, min/target/compileSdk 24/34/34 |
| Android networking | Retrofit 2.11.0, OkHttp 4.12.0, kotlinx.serialization |
| Backend | Python 3.11, FastAPI, SQLModel, SQLite, uvicorn |
| Backend tests | pytest + FastAPI `TestClient` |
| Local execution | Docker / Docker Compose only (no host Python/JDK/Android SDK/Gradle) |
| UI tests | Espresso + Jetpack Compose UI Test (`ui-test-junit4`) in the `androidTest` source set |
| UI test seam | framework-free `AppContainer` service locator backed by MockWebServer (no DI) |
| CI/CD | GitHub Actions: `backend.yml` + `android.yml` |
| Deployment | Manual, isolated Docker Compose on the VPS (separate app dir + Compose project) |

---

## Containerized local development (Docker Desktop only)

Everything runs through Docker/Compose; **no host Python, JDK, Android SDK, or
Gradle is required** (AC41).

### Prerequisites

1. **Docker Desktop** installed and running. There is deliberately **no native
   fallback**.
2. On Apple Silicon (M1/M2/M3), enable **Rosetta 2** for the Android toolchain
   container (Google ships no `linux/aarch64` Android SDK, so `android-build`
   runs as `linux/amd64`):

   ```sh
   softwareupdate --install-rosetta --agree-to-license
   ```

   Then make sure **Docker Desktop → Settings → General → "Use Rosetta for
   x86/amd64 emulation on Apple Silicon"** is enabled. On an Intel host no extra
   step is needed.

> The first `android-build` build is multi-GB and slow (JDK + Android SDK +
> Gradle). Subsequent runs are fast: a named `gradle-cache` volume persists the
> Gradle home between `docker compose run` invocations.

### Backend — run the API

```sh
cp .env.example .env                 # optional; DATABASE_URL has a safe default
docker compose up backend            # foreground; Ctrl-C to stop
docker compose up -d backend         # detached
docker compose ps                    # shows the backend as healthy
docker compose down
```

The API is on <http://localhost:8000>; SQLite persists in `./data`. A Compose
`healthcheck` polls `GET /health`.

### Backend — lint + tests (in the container)

```sh
docker compose run --rm backend-test pytest
docker compose run --rm backend-test sh -c "ruff check . && ruff format --check ."
docker compose run --rm backend-test            # default: ruff + format + pytest
```

### Android — build, JVM unit tests, UI-test compilation (in the container)

```sh
docker compose build android-build
docker compose run --rm android-build ./gradlew assembleDebug
docker compose run --rm android-build ./gradlew testDebugUnitTest
docker compose run --rm android-build ./gradlew lintDebug
docker compose run --rm android-build ./gradlew assembleDebugAndroidTest   # COMPILE UI tests only
```

**The instrumented UI suite runs only in CI (AC42).** Locally you only *compile*
the `androidTest` sources with `assembleDebugAndroidTest`. `connectedDebugAndroidTest`
needs an emulator + KVM, which Docker Desktop on macOS cannot provide; it executes
exclusively in the GitHub Actions Ubuntu + KVM emulator job. A local execution
attempt is unsupported — not a skipped pass.

---

## Testing strategy

Shift-left, layered, and explicit (gate IDs reference the planning acceptance
criteria):

| Layer | What it covers | Where it runs |
|-------|----------------|---------------|
| **Backend — pytest** | `/health`; list/item CRUD; exact `200/201/204/404/422`; empty-name `422`; persistence across restart; OpenAPI; robustness (no 5xx) | `backend-test` container (AC12) **and** `backend.yml` |
| **Android — JVM unit** | ViewModel loading→content / empty / error, toggle, DTO↔domain mapping, snake_case guard | `android-build` container (AC23) **and** `android.yml` `unit` job |
| **Android — instrumented UI (Espresso + Compose)** | create list; add item with quantity; toggle bought; rename/update quantity; delete item (UI-01..UI-05) | **CI only** — `android.yml` `ui-tests` job on an emulator; locally compiled only (AC37–AC40, AC42) |
| **Manual** | loading/empty/error rendering, pull-to-refresh, e2e demo, APK install | human, on an emulator/device — see *Manual checks* |

### CI/CD (GitHub Actions)

- **`backend.yml`** — on PR + `main`: install deps, `ruff check`, `ruff format
  --check`, `pytest`, export OpenAPI artifact, `docker build`, then run the image
  and assert `GET /health` → `200 {"status":"ok"}` (OQ-5 decision).
- **`android.yml`** — on PR + `main`, two jobs:
  - `unit` (fast): `./gradlew testDebugUnitTest assembleDebug` + `demo-apk` artifact;
  - `ui-tests` (merge-blocking): pinned emulator (`api-level: 34`, `target:
    google_apis`, `arch: x86_64`) under KVM runs `./gradlew connectedDebugAndroidTest`.

The emulator UI job is the only place the instrumented suite executes (AC42).

---

## Demo script (AC35)

The documented end-to-end scenario: start the backend → create `Weekend
groceries` → add `milk`, `bread`, `apples` → mark `milk` as bought →
refresh/restart → persisted state.

### A. Backend leg — fully runnable locally

```sh
docker compose up -d backend

BASE=http://localhost:8000
# 1. create the list
curl -s -X POST $BASE/lists -H 'content-type: application/json' \
  -d '{"name":"Weekend groceries"}'
# 2. add items
curl -s -X POST $BASE/lists/<LIST_ID>/items -H 'content-type: application/json' \
  -d '{"name":"milk","quantity":"2 bottles"}'
curl -s -X POST $BASE/lists/<LIST_ID>/items -H 'content-type: application/json' \
  -d '{"name":"bread"}'
curl -s -X POST $BASE/lists/<LIST_ID>/items -H 'content-type: application/json' \
  -d '{"name":"apples","quantity":"6"}'
# 3. mark milk bought
curl -s -X PATCH $BASE/items/<MILK_ID> -H 'content-type: application/json' \
  -d '{"bought":true}'
# 4. read it back
curl -s $BASE/lists/<LIST_ID>
# 5. prove persistence
docker compose restart backend
curl -s $BASE/lists          # the list + items (milk bought) are still there
```

### B. Android leg — manual (no emulator in this environment)

1. Start the backend (`docker compose up -d backend`).
2. Open the app on an emulator (base URL `http://10.0.2.2:8000/`).
3. Create `Weekend groceries`; add `milk` (`2 bottles`), `bread`, `apples`.
4. Mark `milk` as bought; rename it / change quantity; delete an item.
5. Pull to refresh, then restart the app and confirm the persisted state.

The five UI flows above are automated by the instrumented suite and verified in
CI; the interactive walkthrough itself is a manual check here.

---

## Manual checks

These cannot be automated in this environment (no emulator/KVM locally, and the
remote host is not reachable), so they are documented as manual:

- [ ] **Loading state** renders while the lists/details requests are in flight.
- [ ] **Empty state** renders for an empty list (`"No items yet"`-style).
- [ ] **Error state** renders when the backend is unreachable (stop the backend
      and open/refresh the app).
- [ ] **Pull-to-refresh** reloads lists/details after a backend change.
- [ ] **APK install** — download the `demo-apk` CI artifact and install it on an
      emulator/device (`adb install app-debug.apk`).
- [ ] **AC35 end-to-end demo** (section above) reproduced in one sitting.
- [ ] **VPS deploy + smoke check + Hermes untouched** (see `deploy/README.md`).

---

## Deployment (isolated VPS)

The backend is deployed manually as an **isolated** Docker Compose app on the
same VPS as Hermes — separate app directory, separate Compose project
(`-p shared-shopping-list`), a mounted SQLite volume, no committed `.env`, and no
reused Hermes ports/service files. Full runbook:
[`deploy/README.md`](deploy/README.md).

```sh
cd /home/hermes/apps/shared-shopping-list
./deploy/deploy.sh                  # pull → build → up -d → smoke → Hermes verify
./deploy/smoke-check.sh http://127.0.0.1:8000
./deploy/deploy.sh rollback         # redeploy the previous recorded revision
```

The smoke check asserts `GET /health` → `200 {"status":"ok"}` **and** `GET /lists`
→ `200` JSON array. The scripted **Hermes-untouched** evidence (OQ-9) is a
before/after diff of Docker containers, running systemd services, and listening
TCP ports, excluding this app's own project/port — see the deploy README.

> The actual remote deploy, the on-host Hermes verification, and the public
> URL/AC31 step are **pending manual** — no VPS access exists in this environment.

---

## Tradeoffs, scope & non-goals

**Known tradeoffs**

- **SQLite, single file** — simple and portable for the MVP; not a multi-writer
  production store. The schema is frozen for the MVP (no Alembic yet).
- **No authentication / accounts** — sharing is a list-ID/link concept only.
- **Manual production deploy** — deliberately no CI deploy (avoid unsafe
  automation before the MVP is stable); rollback is a documented git-revision
  redeploy.
- **UI execution is CI-only** — Docker Desktop on macOS has no KVM, so the
  `androidTest` suite is compiled locally and executed only in CI (AC42).
- **Public URL deferred (AC31)** — the app ships pointing at the emulator alias
  `10.0.2.2:8000`; a remote mobile demo awaits a decided public URL/subdomain.
- **No DI framework** — a tiny framework-free `AppContainer` service locator is
  the test seam (not Hilt/Koin/Dagger).
- **First Android container build is heavy** — multi-GB toolchain; mitigated by a
  persistent `gradle-cache` volume and pinned versions.

**Explicit non-goals** (out of scope for the MVP): user accounts/authentication,
user management, real-time sync/WebSockets, push notifications, offline-first
sync/conflict resolution, cloud deployment of the app, Firebase/Supabase,
barcode scanning, recipes, grocery catalogs, payments, polished UI/design system,
and heavy architecture/DI frameworks.

**Deferred (post-MVP):** share a list by ID / deep link, mocked API integration
test, generated OpenAPI client/contract check, release workflow, demo video,
offline cache.

---

## Planning docs

Detailed planning material lives under `my-specs/` (git-ignored in this repo):

- `my-specs/docs/Product brief.md` — MVP capabilities, data model, non-goals.
- `my-specs/docs/Architecture notes.md` — repo layout, backend/Android design, testing, CI/CD, deployment.
- `my-specs/docs/Implementation notes.md` — stack specifics, first implementation slice, demo script.
- `my-specs/docs/Roadmap.md` — phased plan and per-phase verification.

`Dashboard.md` (repo root) tracks status, key decisions, and open questions;
`AGENTS.md` holds frozen decisions and scope guardrails.

---

## Interview positioning

Recommended explanation:

> I intentionally kept the product simple: a shared shopping list. The goal was
> not to build a complex app, but to demonstrate a full delivery loop — Android
> client, backend API, automated tests, CI/CD, API contract, and a clear way to
> validate behavior end to end.

The project supports positioning around practical QA/QE leadership, hands-on
engineering credibility, shift-left feedback loops, release readiness, CI/CD
quality gates, and keeping scope small enough to finish and demo.
