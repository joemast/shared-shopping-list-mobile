# Dashboard — Shared Shopping List

## Current status

Project status: Planning repo created

Current status description: The planning docs live in a private GitHub repository (`joemast/shared-shopping-list-mobile`). No implementation code (backend or Android) exists yet.

## Summary

Shared Shopping List is a deliberately simple Android + backend app for managing a shared grocery list. The product is intentionally small so the project can focus on the full delivery loop: Android client, backend API, tests, CI/CD, Docker build, and a repeatable demo.

## Positioning

This project supports a professional profile around:

- hands-on Android/backend exposure;
- QA/QE leadership with practical engineering credibility;
- CI/CD quality gates;
- API and mobile testing;
- release-readiness mindset;
- pragmatic scope control.

## Key decisions

- Use a simple shared shopping list instead of a generic ToDo app.
- Keep the product functionality intentionally small.
- Use FastAPI + SQLite for the backend.
- Use Kotlin + Jetpack Compose for Android.
- Use GitHub Actions for backend and Android checks.
- Develop primarily on Alexander's MacBook Pro M1.
- Use a custom backend, not Firebase or Supabase, because the project should demonstrate API design, backend tests, Docker, deployment, and CI/CD.
- Host the backend on the same VPS that runs Hermes, but as a separate Docker Compose application with isolated directory, port/subdomain, logs, and health check.
- Avoid auth, real-time sync, cloud deployment, and complex architecture in MVP.
- Treat sharing as list ID / link concept, not user accounts.

## Initial documents

- `README.md`
- `Product brief.md`
- `Architecture notes.md`
- `Roadmap.md`

## Interview angle

The app itself is not the point. The useful story is:

> I intentionally kept the product simple: a shared shopping list. The goal was not to build a complex app, but to demonstrate a full delivery loop — Android client, backend API, automated tests, CI/CD, API contract, and a clear way to validate behavior end to end.

## Open questions

- ~~Should the implementation repository be public from the start or private until MVP is ready?~~ Resolved: private (repo `joemast/shared-shopping-list-mobile`).
- Should the backend use SQLModel for faster MVP delivery, or SQLAlchemy directly for more explicit control? Current recommendation: SQLModel for MVP.
- Should Android use Retrofit or Ktor client? Current recommendation: Retrofit for a straightforward REST API demo.
- Which public URL/subdomain should be used for the VPS-hosted backend?
- Should the app be connected to Nanogram interview preparation, or kept as a general hands-on portfolio project?

## Suggested next steps

1. ~~Decide repository name and visibility.~~ Done: `shared-shopping-list-mobile`, private.
2. ~~Create implementation repository.~~ Planning repo created; implementation code still to come.
3. Add backend skeleton and tests first.
4. Add Android skeleton and first screen.
5. Add CI workflows.
6. Add VPS deployment notes and smoke-check script.
7. Record screenshots / short demo once the MVP works.
