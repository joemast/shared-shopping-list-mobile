# Android build/test TOOLCHAIN image (not a runtime) for the Shared Shopping List MVP.
#
# Container-first (AC41): the host needs only Docker Desktop. This image provides a
# JDK + Android SDK cmdline-tools + platform/build-tools + Gradle so that
# `./gradlew assembleDebug | testDebugUnitTest | lintDebug | assembleDebugAndroidTest`
# run without any host JDK / Android SDK / Gradle.
#
# ---------------------------------------------------------------------------
# OQ-16 DECISION (recorded):
#   Platform = linux/amd64 (x86_64) running under Docker Desktop's amd64
#   emulation on the Apple Silicon (M1) host.
#
#   Reason: Google's Android SDK repository publishes NO linux/aarch64 host
#   packages. Verified against repository2-3.xml: every <host-arch>aarch64</host-arch>
#   archive is macOS-only (NDK/emulator); Linux offers only x86 (x64). There is also
#   no aarch64 `commandlinetools-linux-*-latest.zip` (404), and AGP resolves its
#   `aapt2` artifact from the `linux` (x86_64) classifier. An arm64-native Android
#   toolchain is therefore unavailable, so the x86_64/Rosetta fallback is required.
#
#   This M1 host runs the amd64 image at effectively native speed (measured JVM
#   CPU-bound benchmark parity: amd64 1.135s vs arm64 1.161s), so the emulation
#   penalty is negligible here.
# ---------------------------------------------------------------------------
#
# Pins (OQ-16): JDK 17.0.20.1_1, Android cmdline-tools 11076708,
#               platform android-34, build-tools 34.0.0, Gradle 8.7.
FROM --platform=linux/amd64 eclipse-temurin:17.0.20.1_1-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# --- Pinned toolchain versions ---------------------------------------------
ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    ANDROID_CMDLINE_TOOLS_VERSION=11076708 \
    ANDROID_PLATFORM=android-34 \
    ANDROID_BUILD_TOOLS=34.0.0 \
    GRADLE_VERSION=8.7 \
    GRADLE_USER_HOME=/root/.gradle

# --- Android SDK command-line tools ----------------------------------------
RUN mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools" \
    && curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_CMDLINE_TOOLS_VERSION}_latest.zip" \
         -o /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d "${ANDROID_SDK_ROOT}/cmdline-tools" \
    && mv "${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest" \
    && rm /tmp/cmdline-tools.zip

ENV PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

# --- SDK packages + license acceptance -------------------------------------
# `yes |` auto-accepts the SDK licence prompts so the image builds unattended.
RUN yes | sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" --licenses > /dev/null \
    && sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" --install \
         "platform-tools" \
         "platforms;${ANDROID_PLATFORM}" \
         "build-tools;${ANDROID_BUILD_TOOLS}" \
    && rm -rf "${ANDROID_SDK_ROOT}/.temp"

# --- Gradle (bootstraps the committed Gradle wrapper) ----------------------
RUN curl -fsSL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
         -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && rm /tmp/gradle.zip \
    && ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle

# The repo is bind-mounted here at run time (see docker-compose.yml).
WORKDIR /workspace/android

# Default command is a no-op; callers pass an explicit Gradle task, e.g.
#   docker compose run --rm android-build ./gradlew assembleDebug
CMD ["bash"]
