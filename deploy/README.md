# Deployment — Shared Shopping List backend (isolated VPS)

Manual, **fully isolated** Docker Compose deployment of the backend onto the
same VPS that runs **Hermes** (AC29–AC33, build step 11). Production deployment
is intentionally **manual** — there is no CI deploy workflow in the MVP (R3).

> **Execution status in this environment:** the scripts here are authored and
> syntax/runtime-validated where possible, but **no VPS/SSH access or credentials
> exist in this environment**, so the actual remote deploy, the Hermes-untouched
> verification against the real host, and the public-URL step are **manual /
> pending**. See "Verification status" at the bottom.

---

## 1. Isolation model (do not violate)

The backend must never disturb Hermes. Concretely:

| Concern | Shared Shopping List | Hermes (must not change) |
|---------|----------------------|--------------------------|
| App directory | `/home/hermes/apps/shared-shopping-list/` | its own dir |
| Compose project | `-p shared-shopping-list` | its own project name |
| Ports | unique API port (default `8000`) | Hermes gateway ports |
| Env / secrets | host-local `.env` (never committed) | its own env |
| Data | mounted SQLite volume under the app dir | its own data |
| Service files | none shared | unchanged |

`.env` is git-ignored and is created **only on the host** by copying
`.env.example`. No `.env` is ever committed (AC16 / AC29).

---

## 2. First-time host setup

Run on the VPS host (once), as the user that owns `/home/hermes/apps`:

```sh
mkdir -p /home/hermes/apps/shared-shopping-list
cd /home/hermes/apps/shared-shopping-list

# Clone the repo (adjust the remote to your SSH/HTTPS URL if needed).
git clone git@github.com:joemast/shared-shopping-list-mobile.git .

# Create the host-local env file (NOT committed). No secrets required for MVP.
cp .env.example .env
# Optional: move off the default port to avoid any overlap with Hermes.
#   echo 'API_PORT=8000' >> .env        # informational; Compose maps 8000:8000

chmod +x deploy/deploy.sh deploy/smoke-check.sh
```

Check the Compose file renders before touching anything:

```sh
cd /home/hermes/apps/shared-shopping-list
docker compose -p shared-shopping-list config >/dev/null && echo "compose config OK"
```

---

## 3. Deploy (pull → build → restart → smoke → verify)

From the app directory:

```sh
cd /home/hermes/apps/shared-shopping-list
./deploy/deploy.sh
```

`deploy.sh` performs, in order:

1. records the **previous git revision** (rollback target, AC32);
2. captures the **Hermes "before" snapshot** if no baseline exists (OQ-9);
3. `git fetch` + checkout/pull of `GIT_REF` (default `main`);
4. `docker compose -p shared-shopping-list build --pull`;
5. `docker compose -p shared-shopping-list up -d --remove-orphans`;
6. runs `deploy/smoke-check.sh` against `http://127.0.0.1:8000` with bounded
   retries (AC30 / HP-14);
7. captures the **Hermes "after" snapshot** and diffs it against the baseline
   (AC29 / AC33 / OQ-9).

Individual steps are also available:

```sh
./deploy/deploy.sh baseline   # capture Hermes before-snapshot
./deploy/deploy.sh verify     # capture after-snapshot and diff
./deploy/deploy.sh status     # compose ps + recent logs
./deploy/deploy.sh logs       # follow backend logs
./deploy/deploy.sh rollback   # redeploy the previously recorded revision
```

Environment overrides: `APP_DIR`, `COMPOSE_PROJECT`, `API_PORT`, `GIT_REF`,
`HEALTH_URL`, `BASELINE_FILE`.

### Manual equivalent (if you prefer explicit commands)

```sh
cd /home/hermes/apps/shared-shopping-list
git fetch --prune origin && git pull --ff-only origin main
docker compose -p shared-shopping-list build --pull
docker compose -p shared-shopping-list up -d --remove-orphans
./deploy/smoke-check.sh http://127.0.0.1:8000
```

---

## 4. Smoke check (AC30 / AC32 / HP-14)

`deploy/smoke-check.sh` asserts the **locked** contract:

- `GET /health` → **HTTP 200 AND body exactly `{"status":"ok"}`**;
- `GET /lists` → **HTTP 200 AND a JSON array** (`[]` or `[{...}]`).

It is parameterizable and exits non-zero on any mismatch, so it can gate the
deploy and be re-run any time:

```sh
./deploy/smoke-check.sh                              # defaults to http://localhost:8000
./deploy/smoke-check.sh http://127.0.0.1:8000        # on the VPS
BASE_URL=http://localhost:8000 ./deploy/smoke-check.sh
```

Expected output on success:

```text
Smoke check against http://127.0.0.1:8000
PASS: GET /health -> HTTP 200 {"status":"ok"}
PASS: GET /lists -> HTTP 200 JSON array
Smoke check OK: http://127.0.0.1:8000
```

> **AC31 (Android → VPS URL)** stays **deferred** for the MVP: the app ships with
> the emulator base URL `http://10.0.2.2:8000/`. Pointing a build at the VPS URL
> requires a decided public URL/subdomain (still open), so it is out of scope
> until then.

---

## 5. Isolation checklist (AC29)

Run these on the host and keep the output as evidence:

```sh
# 5.1 Separate app directory + separate Compose project (labels prove isolation)
docker compose -p shared-shopping-list --project-directory /home/hermes/apps/shared-shopping-list ps
docker inspect --format '{{index .Config.Labels "com.docker.compose.project"}}' \
  "$(docker compose -p shared-shopping-list ps -q | head -n1)"

# 5.2 The app's container belongs only to the shared-shopping-list project
docker ps --filter "label=com.docker.compose.project=shared-shopping-list"

# 5.3 Mounted SQLite volume persists on the host
ls -l /home/hermes/apps/shared-shopping-list/data/

# 5.4 No committed .env in the repo
git -C /home/hermes/apps/shared-shopping-list status --porcelain | grep -q '\.env$' \
  && echo "FAIL: .env tracked" || echo "OK: no .env tracked"

# 5.5 No reused Hermes service files (nothing outside the app dir was touched)
git -C /home/hermes/apps/shared-shopping-list status   # clean working tree
```

Checklist result (to be filled on the real host):

- [ ] App dir is `/home/hermes/apps/shared-shopping-list/`
- [ ] Compose project label is `shared-shopping-list`
- [ ] SQLite file present under `data/` and persists across `restart`
- [ ] No `.env` committed / tracked
- [ ] Unique API port; no Hermes port reused
- [ ] Hermes containers/services/ports unchanged (see §6)

---

## 6. OQ-9 — proving Hermes is untouched (AC29 / AC33)

**Decision (OQ-9):** the repeatable evidence is a **before/after snapshot diff**
of (a) running Docker containers, (b) running systemd services, and (c) listening
TCP ports — with this app's own Compose project and API port **excluded**. If the
diff is empty, Hermes was not disturbed.

`deploy.sh` automates exactly this (`baseline` → `verify`). The raw commands are:

```sh
# --- BEFORE (run before deploying) ---
docker ps --format '{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}' \
  | grep -v -- 'shared-shopping-list' | sort > /tmp/hermes-before-docker.txt
systemctl list-units --type=service --state=running --no-pager --no-legend \
  | awk '{print $1}' | sort -u > /tmp/hermes-before-services.txt
ss -ltnH | awk '{print $4}' | grep -v -- ':8000$' | sort -u > /tmp/hermes-before-ports.txt

# --- AFTER (run after deploy + smoke check) ---
docker ps --format '{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}' \
  | grep -v -- 'shared-shopping-list' | sort > /tmp/hermes-after-docker.txt
systemctl list-units --type=service --state=running --no-pager --no-legend \
  | awk '{print $1}' | sort -u > /tmp/hermes-after-services.txt
ss -ltnH | awk '{print $4}' | grep -v -- ':8000$' | sort -u > /tmp/hermes-after-ports.txt

# --- DIFF (must be empty) ---
diff -u /tmp/hermes-before-docker.txt   /tmp/hermes-after-docker.txt
diff -u /tmp/hermes-before-services.txt /tmp/hermes-after-services.txt
diff -u /tmp/hermes-before-ports.txt    /tmp/hermes-after-ports.txt
```

One-line convenience wrapper (same as `deploy.sh verify`):

```sh
cd /home/hermes/apps/shared-shopping-list && ./deploy/deploy.sh verify
```

**Expected:** all three `diff` commands print nothing and the wrapper reports
"Hermes untouched". Capture the output as the paired AC29/AC33 evidence.

> **Pending manual:** this verification must be run on the real VPS host, which
> is not reachable from this environment.

---

## 7. Rollback (AC32)

Rollback path: **redeploy the previously recorded git revision** (the script
records it automatically before each deploy in `.previous-revision`).

```sh
cd /home/hermes/apps/shared-shopping-list
./deploy/deploy.sh rollback
```

Equivalent manual steps:

```sh
cd /home/hermes/apps/shared-shopping-list
git checkout --detach "$(cat .previous-revision)"
docker compose -p shared-shopping-list build --pull
docker compose -p shared-shopping-list up -d --remove-orphans
./deploy/smoke-check.sh http://127.0.0.1:8000
```

If only the image regressed, an alternative is to repoint the service at the
previous image tag and `up -d`. The SQLite data volume is preserved across
rollbacks (schema is frozen for the MVP — no Alembic, R5).

---

## 8. Log inspection

```sh
cd /home/hermes/apps/shared-shopping-list

# Follow the backend logs
docker compose -p shared-shopping-list logs -f backend

# Last 100 lines
docker compose -p shared-shopping-list logs --tail=100 backend

# Container status / health
docker compose -p shared-shopping-list ps
```

Manual-deploy verification (Architecture notes "Manual deployment should
verify"): repository revision, image builds, container starts, `/health` OK, and
no startup errors in the logs.

---

## 9. Verification status (this environment)

| Item | Status |
|------|--------|
| `deploy/deploy.sh` authored, `bash -n` clean | ✅ done |
| `deploy/smoke-check.sh` authored, runs locally against the Compose backend | ✅ done (see repo README) |
| Local smoke check: `docker compose up -d backend` → smoke passes → stop | ✅ done |
| Real VPS deploy (`pull/build/restart`) | ⏳ **pending manual** (no VPS/SSH here) |
| Hermes-untouched verification on the real host (OQ-9 evidence) | ⏳ **pending manual** |
| Public URL / AC31 (app → VPS URL) | ⏳ **deferred** (URL undecided) |
