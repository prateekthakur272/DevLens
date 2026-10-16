<p align="center">
  <img src="desktopApp/icons/app.png" alt="DevLens" width="112">
</p>

<h1 align="center">DevLens</h1>

<p align="center">
  A local-first desktop tool to inspect, audit and reverse engineer Android <code>.apk</code> and <code>.aab</code> files.
</p>

<p align="center">
  Kotlin Multiplatform · Compose Multiplatform · macOS / Windows / Linux
</p>

---

## Overview

Open an APK or an App Bundle and DevLens analyzes it on your machine — nothing is uploaded. You get a security
score, a table of findings mapped to OWASP MASVS and CWE, the decoded manifest, permissions, signing certificate,
size breakdown, DEX class browser and hardcoded-secret scan. One click decompiles the app to Java and opens it in a
built-in code editor, and the result can be exported as a formatted PDF report.

## Screenshots

<img width="1470" height="923" alt="Screenshot 2026-10-16 at 5 27 26 PM" src="https://github.com/user-attachments/assets/8b0a564c-5d0e-4bb4-a160-7e229beb0989" />
<img width="1470" height="923" alt="Screenshot 2026-10-16 at 5 28 11 PM" src="https://github.com/user-attachments/assets/0b8f7c4f-0b57-414f-a307-d36dc0b950a6" />
<img width="1470" height="923" alt="Screenshot 2026-10-16 at 5 28 16 PM" src="https://github.com/user-attachments/assets/ff72023c-dfeb-455b-9425-d6a6d1318fc5" />
<img width="1470" height="923" alt="Screenshot 2026-10-16 at 5 28 30 PM" src="https://github.com/user-attachments/assets/662594c7-1df5-478a-87e5-4707e7a4b27a" />


## Features

**Open**
- `.apk` and `.aab` files, from the file dialog, **File ▸ Open**, or by dragging a file onto the window.
- App name and launcher icon are read from the file (including bundles with shortened resource paths).

**Analyze** (tabs)
- **Overview** — risk score, severity breakdown, APK information, findings with a detail panel, attack surface,
  possible secrets and dangerous permissions.
- **Manifest** — application flags, activities, services, receivers and providers with exported state, permissions
  and intent-filter counts.
- **Permissions** — every requested permission classified by risk.
- **Security** — all findings, each with evidence, impact, recommendation, category and standards reference.
- **Size** and **Structure** — size by category, largest files, full file list.
- **Classes** — searchable DEX class list (no decompiler needed).
- **Secrets** — API keys, tokens, private keys and credentials found in DEX, native libraries, resources and assets.
- **Signing** — signature schemes and certificate details (APK Signature Schemes via `apksigner`; JAR signature for bundles).

**Decompile**
- Reverse engineers the app to Java sources and decoded resources with [JADX](https://github.com/skylot/jadx).
- Opens in a separate code-editor window: file tree with search, tabs, syntax highlighting for Java/Kotlin, XML and JSON,
  line numbers, selectable text and adjustable font size.

**Report**
- Export as a formatted **PDF**, or as Markdown, JSON or plain text.
- Files are suggested as `report_<app>_<version>_<yyyy-MM-dd>`.

**Desktop niceties**
- Native menu bar with keyboard shortcuts, a settings window (theme, Android SDK path, editor font size),
  and light/dark themes.

## Security rules

Findings come from deterministic rules, never from AI. There are 40 rule IDs, each mapped to an
[OWASP MASVS](https://mas.owasp.org/MASVS/) control and a [CWE](https://cwe.mitre.org/):

| Area | What is checked |
|---|---|
| Manifest | Debuggable, test-only, unrestricted backup, outdated `targetSdk` / `minSdk`, legacy external storage |
| Components | Exported activities, services, receivers and providers without a permission; URI-granting providers; task hijacking; weak custom permissions; custom-scheme and unverified deep links |
| Network | Cleartext traffic, missing Network Security Config, no certificate-pinning indicators |
| Permissions | SMS, call logs, background location, all-files access, overlays, package install, accessibility / device admin, device identifiers, app enumeration, system settings |
| Signing | Debug certificate, no APK Signature Scheme v2+ (APKs), RSA key shorter than 2048 bits |
| Secrets | Hardcoded keys, tokens, private keys and credentials, grouped by kind |
| Files & code | Keystores / `.env` / database dumps in the package, embedded executable code, Flutter debug builds, debug tooling (Stetho, LeakCanary, Chucker…), unobfuscated code, 32-bit-only native libraries |

The rules live in [`SecurityRules.kt`](shared/src/commonMain/kotlin/dev/prateekthakur/devlens/domain/security/SecurityRules.kt)
and are covered by unit tests. A few rules are heuristics (obfuscation, certificate pinning, debug libraries) and can be
wrong for unusual apps. DevLens is an aid for review, not a substitute for a security audit.

## Requirements

- **JDK 17 or newer** to build and run (a JDK 21 toolchain is downloaded automatically if needed).
- **Android SDK Build-Tools** (`aapt2`, `apksigner`) for the full analysis. DevLens finds the SDK through
  `ANDROID_HOME`, `ANDROID_SDK_ROOT` or the default location, or you can set a path in **Settings**.
  Without them, manifest and signing analysis are unavailable.

## Run

```bash
./gradlew :desktopApp:run                  # run the app
./gradlew :desktopApp:hotRun --auto        # run with hot reload
```

## Build an installer

```bash
./gradlew :desktopApp:packageDmg           # macOS  → desktopApp/build/compose/binaries/main/dmg/
./gradlew :desktopApp:packageReleaseDmg    # macOS release build → .../main-release/dmg/
./gradlew :desktopApp:packageMsi           # Windows
./gradlew :desktopApp:packageDeb           # Linux
```

Installers are built for the machine you build on (an Apple Silicon Mac produces an arm64 build). They are not
signed or notarized; on another Mac the first launch needs **right-click ▸ Open**. The release build ships the
libraries unshrunk because JADX loads its plugins by reflection.

## Tests

```bash
./gradlew :shared:jvmTest
```

## Keyboard shortcuts

Use **Ctrl** instead of **⌘** on Windows and Linux.

| Shortcut | Action |
|---|---|
| ⌘O | Open APK or AAB |
| ⌘R | Re-analyze |
| ⌘D | Decompile / open code editor |
| ⌘F | Show findings |
| ⌘1 – ⌘9 | Go to tab |
| ⌘, | Settings |
| ⌘= / ⌘- / ⌘0 | Code editor font size: increase / decrease / reset |

## Project structure

```
shared/                       Kotlin Multiplatform module (JVM target)
  src/commonMain/             Domain models, security rules, UI (Compose), report models
    domain/                   apk, manifest, permission, security, secrets, signing, size, decompile, report, …
    infrastructure/           aapt2 / apksigner wrappers, SDK locator
    ui/apk/                   Analysis screen, tabs, code editor
    ui/settings/              Settings window
  src/jvmMain/                APK/AAB readers, DEX parser, JADX decompiler, PDF writer, icon reader
  src/commonTest, jvmTest/    Unit tests
desktopApp/                   Desktop entry point: windows, menu bar, drag and drop, packaging
docs/screenshots/             README screenshots
```

Settings are stored in `~/.devlens/settings.properties`. Decompiled sources are written to a temporary folder.

## Built with

[Kotlin](https://kotlinlang.org/) · [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) ·
[JADX](https://github.com/skylot/jadx) · [Apache PDFBox](https://pdfbox.apache.org/) ·
Android SDK `aapt2` and `apksigner`

## Privacy

DevLens is local-first. APKs, decompiled sources and reports are processed on your machine and never uploaded.

## Author

Developed by [Prateek Thakur](https://github.com/prateekthakur272).

## License

No license has been specified yet. Add a `LICENSE` file and update this section before publishing the project.
