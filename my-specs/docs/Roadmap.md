# Roadmap — Shared Shopping List

## Phase 0 — Project framing

Goal: capture the idea and keep scope small.

Deliverables:

- Project notes in MyProjects.
- MVP scope.
- Technology choices.
- Interview positioning.
- Non-goals.

Status: done.

## Phase 1 — Implementation repository setup

Goal: create the working GitHub repository and skeleton.

Deliverables:

- Separate implementation repository, likely private at first.
- `README.md` with goal and demo scenario.
- Backend folder.
- Android folder.
- GitHub Actions folder.
- Initial issue list or project board.
- Local development instructions for MacBook Pro M1.
- Decision record explaining why the project uses a custom backend instead of Firebase/Supabase.

Decision needed:

- Repository name: `shared-shopping-list`, `shopping-list-demo`, or `android-backend-shopping-list`.
- Public vs private during early development.
- Backend public URL/subdomain for VPS deployment.

## Phase 2 — Backend MVP

Goal: implement the API first, with tests.

Deliverables:

- FastAPI app.
- SQLite persistence.
- `ShoppingList` and `ShoppingItem` models.
- CRUD endpoints for lists and items.
- pytest API tests.
- OpenAPI available locally.
- Dockerfile.
- Docker Compose for local run.
- `.env.example` without secrets.
- Health endpoint and smoke-check command.

Verification:

- `pytest` passes.
- API works through local Swagger UI.
- Docker container starts and passes health check.
- OpenAPI schema is available and can be exported.

## Phase 3 — Android MVP

Goal: create a small Android app connected to the backend.

Deliverables:

- Kotlin + Jetpack Compose app.
- Lists screen.
- List details screen.
- Add item flow.
- Toggle bought state.
- Delete item.
- Loading / empty / error states.
- ViewModel tests.

Verification:

- App builds locally.
- App can talk to local backend.
- Demo scenario works on emulator.

## Phase 4 — CI/CD quality gates

Goal: make quality feedback visible on GitHub.

Deliverables:

- Backend GitHub Actions workflow.
- Android GitHub Actions workflow.
- Docker build check.
- APK artifact upload.
- Optional exported OpenAPI artifact.

Verification:

- Pull request shows backend tests passing.
- Pull request shows Android tests/build passing.
- Main branch produces build artifacts.

## Phase 4.5 — VPS backend deployment

Goal: host the custom backend on the VPS without interfering with Hermes.

Deliverables:

- Separate VPS app directory, for example `/home/hermes/apps/shared-shopping-list/`.
- Docker Compose deployment for backend and SQLite volume.
- Manual deploy script or documented deploy command.
- Health check endpoint exposed on a known URL/port.
- Smoke-check script.
- Basic log inspection command.
- Rollback note.

Verification:

- Deployed backend returns `200 OK` from `/health`.
- `GET /lists` returns JSON.
- Android app can use the VPS API URL in dev/demo configuration.
- Hermes gateway/services remain untouched.

## Phase 5 — Interview-ready documentation

Goal: make the project easy to discuss and demo.

Deliverables:

- README with architecture diagram or simple text architecture.
- Demo script.
- CI/CD explanation.
- Testing strategy section.
- Known tradeoffs / non-goals.
- Screenshots or short GIF/video if useful.

Demo script:

1. Open GitHub Actions and show checks.
2. Start backend locally.
3. Open Android app.
4. Create `Weekend groceries`.
5. Add items.
6. Toggle one item as bought.
7. Restart/refresh and show state persisted.
8. Explain what was intentionally left out.

## Phase 6 — Optional enhancements

Add only if the MVP is already done and demoable.

Possible additions:

- Share list by ID / deep link.
- Simple OpenAPI contract check.
- Basic Compose UI test.
- Lightweight release workflow.
- Public demo video.
- Cloud deployment for backend.
- Offline cache.

Avoid adding these before the MVP is finished.
