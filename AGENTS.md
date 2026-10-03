# AGENTS.md — Shared Shopping List (planning repo)

## What this repo is

Planning/notes repository only. It contains **no source code, no package manifests, and no build or test tooling**. Do not run `npm`/`gradle`/`pytest`, do not look for an app entrypoint, and do not assume the planned stack already exists.

It is now a git repo on GitHub: `joemast/shared-shopping-list-mobile` (private, default branch `main`, remote `origin` over SSH). Only planning docs exist so far.

## Source-of-truth docs

Read the relevant file before acting; they overlap but each owns a topic:

- `Dashboard.md` — current status, key decisions, open questions (repo root).
- `my-specs/docs/Product brief.md` — MVP capabilities, data model, non-goals.
- `my-specs/docs/Architecture notes.md` — planned repo layout, backend/Android design, testing strategy, CI/CD, VPS deployment.
- `my-specs/docs/Implementation notes.md` — stack specifics, first implementation slice (build order), scope guardrails.
- `my-specs/docs/Roadmap.md` — phased plan and per-phase verification.
- `README.md` — product framing and interview positioning (repo root).

The remaining docs live under `my-specs/docs/`; `AGENTS.md`, `README.md`, and `Dashboard.md` stay in the repo root.

Status: idea captured, nothing implemented. Docs describe the target, not current reality.

## Frozen decisions (do not re-litigate)

- Android: Kotlin + Jetpack Compose, Retrofit, ViewModel.
- Backend: Python 3.11+, FastAPI, SQLite, SQLModel.
- Tests: pytest (backend), ViewModel/DTO unit tests (Android).
- CI/CD: GitHub Actions; backend lint/tests + Docker build; Android Gradle tests/build + APK artifact.
- Backend hosted on the same VPS as Hermes but **fully isolated**: separate app dir, separate Compose project, no reused ports/service files, no committed `.env`, mounted SQLite volume, `/health` smoke check.
- Sharing is a list-ID/link concept, not accounts.

## MVP scope guardrails

Do not add auth, user management, real-time sync, push notifications, offline sync, barcode/recipes, cloud deploy, or heavy DI/architecture frameworks. List-ID sharing only.

## Still-open decisions (treat recommendations as provisional, don't silently finalize)

- Repo public vs private during early development.
- SQLModel vs raw SQLAlchemy.
- Retrofit vs Ktor client.
- Public URL/subdomain for the VPS backend.

Per `Dashboard.md`, current recommendations are SQLModel and Retrofit; "recommendation" ≠ decision.

## Build order (when implementation starts)

Follow the first slice in `my-specs/docs/Implementation notes.md`: backend `/health` + pytest → list/item models and CRUD tests → Dockerfile/Compose → backend CI → Android skeleton → Retrofit/repository → ViewModel tests → Android CI → VPS deploy script + smoke check. Do not start Android UI polish before backend API and CI are stable.

## Gotchas worth remembering

- Android emulator reaches a host-machine backend at `http://10.0.2.2:<port>`, not `localhost`.
- Do not automate production deployment before the MVP is stable; start with a manual deploy script and verify `/health` + `GET /lists`.
- Keep scope small enough to finish and demo — that is the explicit project goal.
