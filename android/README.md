# Android client + build container

Container-first Android client for the shared shopping list. The host needs only
Docker Desktop — there is no host JDK, Android SDK, or Gradle.

## Stage 4 recorded decisions

### OQ-16 — `android-build` base image + toolchain pinning (DECIDED)

**Decision:** `linux/amd64` (x86_64) toolchain image running under Docker
Desktop's amd64 emulation on the Apple Silicon (M1) host.

**Reason:** Google's Android SDK repository publishes **no linux/aarch64 host
packages**. This was verified against `repository2-3.xml`: every
`<host-arch>aarch64</host-arch>` archive is macOS-only (NDK / emulator); Linux
offers only `x64`/`x86`. There is also no
`commandlinetools-linux-*-aarch64-latest.zip` (404), and AGP resolves its
`aapt2` artifact from the `linux` (x86_64) classifier. An arm64-native Android
toolchain is therefore **unavailable**, so the x86_64 fallback is required.

Measured on this M1 host, the amd64 image runs at effectively native speed
(JVM CPU-bound benchmark: amd64 1.135s vs arm64 1.161s), so the emulation
penalty is negligible.

**Pins:**

| Component | Pin |
|-----------|-----|
| Base image | `eclipse-temurin:17.0.20.1_1-jdk-jammy` (linux/amd64) |
| JDK | Temurin 17.0.20.1+1 |
| Android cmdline-tools | `11076708` |
| Android platform | `android-34` |
| Android build-tools | `34.0.0` |
| Gradle | `8.7` |

### OQ-4 — Android toolchain pins + JSON library (DECIDED)

**JSON library:** `kotlinx.serialization` (first-party Kotlin support).

| Component | Pin |
|-----------|-----|
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24 |
| Compose Compiler extension | 1.5.14 (pinned pair with Kotlin 1.9.24) |
| Compose BOM | 2024.06.00 |
| Gradle | 8.7 |
| JDK (source/target/jvmTarget) | 17 |
| minSdk / targetSdk / compileSdk | 24 / 34 / 34 |
| Retrofit | 2.11.0 |
| kotlinx.serialization-json | 1.6.3 |
| OkHttp (logging + mockwebserver) | 4.12.0 |

### OQ-14 — emulator image pin for the Stage 7 CI UI job (RESOLVED at Stage 4)

Pinned to the app's `compileSdk`/`targetSdk` (34):

| `ReactiveCircus/android-emulator-runner` input | Pinned value |
|------------------------------------------------|--------------|
| `api-level` | `34` |
| `target` | `google_apis` |
| `arch` | `x86_64` |

System image: `system-images;android-34;google_apis;x86_64`.

## Local Android commands (Docker Desktop only)

```text
docker compose build android-build

docker compose run --rm android-build ./gradlew assembleDebug
docker compose run --rm android-build ./gradlew testDebugUnitTest
docker compose run --rm android-build ./gradlew lintDebug
docker compose run --rm android-build ./gradlew assembleDebugAndroidTest   # COMPILE UI tests only
```

**UI-test execution is CI-only.** `assembleDebugAndroidTest` only *compiles* the
`androidTest` sources. `connectedDebugAndroidTest` runs **exclusively** in the
GitHub Actions Ubuntu + KVM emulator job (Stage 7); Docker Desktop on macOS has
no KVM, so a local execution attempt is unsupported — not a skipped pass (AC42).

## Module layout

```text
app/src/main/java/com/example/sharedshoppinglist/
├── MainActivity.kt            # single activity; builds AppContainer
├── AppContainer.kt            # framework-free service locator (test seam, no DI)
├── SharedShoppingListApp.kt   # Application
├── domain/                    # ShoppingList, ShoppingItem, UiState
├── data/                      # ApiService, ApiClient, DTOs, mapper, repository
├── viewmodel/                 # ListsViewModel, ListDetailsViewModel + factories
└── ui/                        # AppRoot, ListsScreen, ListDetailsScreen, components
app/src/debug/AndroidManifest.xml
app/src/androidTest/           # instrumented UI suite source set (Stage 6)
```

`AppContainer` exposes the base URL (`BuildConfig.API_BASE_URL`,
`http://10.0.2.2:8000/` for the emulator) and the `ShoppingRepository`. The
instrumented suite installs a MockWebServer-backed override via
`AppContainer.installTestOverride(...)`; nothing else in the app knows about tests.
