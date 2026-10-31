# GitHub Actions APK CI/CD Workflow Upgrade

Upgrade and fix the GitHub Actions workflow in `.github/workflows/build.yml` so that GitHub automatically and reliably compiles both Debug and Release APKs, attaches them as downloadable artifacts, and publishes them to GitHub Releases.

### User Review & Critical Decisions

> [!IMPORTANT]
> The build workflow will automatically synthesize the required keystores (`debug.keystore` and `my-upload-key.jks`) within the runner using JDK `keytool`, eliminating missing keystore build failures while outputting fully signed, installable APKs.

- **Confirmed Decisions**:
  - **APK Variants**: Generate both **Debug APK** and **Signed Release APK**.
  - **Distribution**: Upload both APKs as downloadable workflow artifacts (`actions/upload-artifact@v4`) and automatically draft/publish a **GitHub Release** (`softprops/action-gh-release@v2`) when a version tag (`v*`) is pushed or triggered via workflow dispatch.
  - **Signing & Keystore Generation**: Automate `keytool` creation for `debug.keystore` and `my-upload-key.jks` with standard signing parameters matching `app/build.gradle.kts`.
  - **Secrets & Environment**: Automatically provision `.env` from `.env.example` to satisfy the Secrets Gradle plugin.

---

### 1. Overview & Core Concept

- **What It Does**: Provides a rock-solid, production-ready GitHub Actions workflow that compiles OmniConvert on GitHub's cloud runners (`ubuntu-latest`) using Temurin JDK 21 and Gradle 9.3.1.
- **Key Value**: Enables users and collaborators to download ready-to-install APK files directly from GitHub commits, pull requests, and releases without needing a local Android development environment.

---

### 2. User Experience & CI/CD Flow

```
┌────────────────────────────────────────────────────────┐
│               GitHub Push / Tag / Dispatch             │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    GitHub Actions Runner               │
│  1. Checkout repository & setup JDK 21 (Temurin)      │
│  2. Setup Gradle 9.3.1 & generate wrapper              │
│  3. Initialize .env file from .env.example             │
│  4. Generate debug.keystore & release keystore         │
│  5. Run: ./gradlew assembleDebug assembleRelease       │
└───────────────────────────┬────────────────────────────┘
                            │
           ┌────────────────┴────────────────┐
           │                                 │
┌──────────▼───────────────┐       ┌─────────▼──────────────┐
│  Upload Workflow Artifact│       │ GitHub Release Publish │
│  (Debug & Release APKs)  │       │ (When version tag or   │
│                          │       │  dispatch is triggered)│
└──────────────────────────┘       └────────────────────────┘
```

#### Step-by-Step Execution Sequence
1. **Repository Checkout**: Fetch source code using `actions/checkout@v4`.
2. **Java 21 Toolchain**: Install Eclipse Temurin JDK 21 via `actions/setup-java@v4`.
3. **Gradle Configuration**: Setup Gradle 9.3.1 caching using `gradle/actions/setup-gradle@v4`.
4. **Environment Initialization**: Copy `.env.example` to `.env` to prevent Secrets plugin validation errors.
5. **On-the-Fly Keystore Provisioning**:
   - Generate `debug.keystore` (`storepass=android`, `keypass=android`, `alias=androiddebugkey`).
   - Generate `my-upload-key.jks` (`storepass=android`, `keypass=android`, `alias=upload`).
   - Export signing passwords in step environment variables matching `app/build.gradle.kts`.
6. **Parallel APK Compilation**:
   - Run `chmod +x ./gradlew`
   - Execute `./gradlew assembleDebug assembleRelease --no-daemon --stacktrace`
7. **Artifact Publishing**:
   - Collect output APKs from `app/build/outputs/apk/debug/*.apk` and `app/build/outputs/apk/release/*.apk`.
   - Upload both artifacts named `OmniConvert-APKs`.
8. **Automated GitHub Release (Optional/Tag-based)**:
   - When triggered on tag push (`refs/tags/v*`) or when manually dispatched, attach APK binaries directly to the GitHub Release.

---

### 3. Key Technical Decisions & Trade-Offs

- **Decision 1: Generating Keystores in Runner vs Requiring Committed Secret Keys**
  - *Chosen Approach*: Generate clean signing keystores directly in the workflow step using `keytool`.
  - *Why*: Public repositories and forks will build immediately without requiring users to manually configure base64 secrets in repository settings, while still producing fully signed, installable APKs.
- **Decision 2: Universal Path Matching for Upload Artifacts**
  - *Chosen Approach*: Explicitly match `app/build/outputs/apk/**/*.apk`.
  - *Why*: Accommodates both debug and release output paths across various Gradle versions without missing files.

---

### 4. Technical Architecture *(Workflow Specification)*

#### Workflow File: `.github/workflows/build.yml`
- **Triggers**:
  - `push: branches: [ "main" ]`
  - `push: tags: [ "v*" ]`
  - `workflow_dispatch: inputs: { create_release: { description: "Publish as GitHub Release?", type: boolean, default: false } }`
- **Permissions**: `contents: write` (for release publishing)
- **Target outputs**:
  - `app/build/outputs/apk/debug/app-debug.apk`
  - `app/build/outputs/apk/release/app-release.apk`
