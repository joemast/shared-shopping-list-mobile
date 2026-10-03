# Shared Shopping List

Shared Shopping List is a small Android + backend pet project for demonstrating a complete delivery loop: mobile client, backend API, automated tests, CI/CD checks, Docker build, and a simple end-to-end demo flow.

The product itself is intentionally simple. The goal is not to build a complex shopping app, but to create a compact hands-on portfolio project that shows practical engineering and quality workflow across Android, backend, and GitHub Actions.

## Problem

A generic ToDo app is easy to understand but too abstract for a useful portfolio demo. A shared shopping list keeps the same low functional complexity while adding a small real-world collaboration scenario:

- one list can be used by more than one person;
- items can be added, marked as bought, renamed, or removed;
- the Android app stores state through a backend API;
- the implementation can show API contracts, tests, CI checks, and build artifacts.

## Product idea

The app lets a user create or open a shopping list, add grocery items, mark them as bought, and refresh the list from the backend.

The first version should treat sharing as a simple list ID / link concept, not as a full account system.

## Stack decision

The preferred implementation stack is native Android with a custom backend:

- Android: Kotlin + Jetpack Compose.
- Backend: FastAPI + SQLite + SQLModel.
- Backend hosting: Docker Compose on the VPS, isolated from Hermes.
- CI/CD: GitHub Actions for backend tests, Android build/tests, Docker build, and later deployment support.

Firebase and Supabase were considered, but the MVP should use a custom backend because the main purpose is to demonstrate the full Android/backend/API/testing/deployment loop rather than only a fast mobile CRUD app.

## Primary purpose

This project is a hands-on demo for interviews and professional positioning. It should demonstrate:

- Android development with Kotlin and Jetpack Compose;
- backend API development with FastAPI;
- client-server integration;
- automated tests on both backend and Android sides;
- GitHub Actions CI/CD quality gates;
- Dockerized backend build;
- a clear repeatable demo scenario.

## MVP scope

- Create a shopping list.
- View shopping lists.
- Add an item to a list.
- Mark an item as bought / not bought.
- Rename or delete an item.
- Persist data through the backend.
- Show loading and error states in the Android app.
- Run backend and Android checks in GitHub Actions.
- Upload Android APK and backend Docker build result / metadata as CI artifacts where useful.

## Non-goals for the first version

- User accounts.
- Authentication.
- Real-time sync.
- Push notifications.
- Cloud deployment.
- Fancy UI design.
- Offline-first conflict resolution.
- Complex architecture frameworks.
- Firebase/Supabase backend-as-a-service implementation.

## Interview positioning

Recommended explanation:

> I intentionally kept the product simple: a shared shopping list. The goal was not to build a complex app, but to demonstrate a full delivery loop — Android client, backend API, automated tests, CI/CD, API contract, and a clear way to validate behavior end to end.

For a Nanogram-style mobile/backend role, the project can be positioned as a small sandbox to touch the Android/backend/CI/CD shape of the work, not as a direct copy of the product domain.

## Related portfolio themes

This project supports Alexander's positioning around:

- practical QA/QE leadership;
- hands-on engineering credibility;
- shift-left feedback loops;
- release readiness;
- CI/CD quality gates;
- Android/backend/API testing;
- keeping scope small enough to finish and demo.
