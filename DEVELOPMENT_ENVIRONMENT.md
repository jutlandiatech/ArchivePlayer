# Development environment

Observed November 2026. Historical entries stay; new findings are appended with a date.

## Toolchain (from the repository)

| Item | Value |
|---|---|
| Gradle wrapper | 9.9.0 |
| Android Gradle Plugin | 9.5.1 |
| Kotlin | 2.4.21 (Compose compiler plugin; `core` uses Kotlin/JVM) |
| Compose BOM | 2026.09.00, Material 3 |
| Media3 | 1.11.1 (ExoPlayer, Compose UI, Cast) |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26; build tools 37.0.0 |
| Java target | 17 |

## Thomas's Windows PC

- Checkout: `E:\repo\ArchivePlayer`.
- Android SDK: `C:\Users\thoma\AppData\Local\Android\Sdk`; `ANDROID_HOME` points there.
- Builds and Android checks run here; `tools/run-gradle.ps1` wraps `gradlew.bat`.

## Devin VM

- No Android SDK; used for specs, mockups and core-only work.
- Maven Central answered HTTP 429 from the VM. A VM-only Gradle init script redirects Maven Central to Google's mirror; it is not part of the repository and not needed on normal machines.
