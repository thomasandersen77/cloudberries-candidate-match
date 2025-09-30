# Java versioning and SDKMAN usage (JDK policy)

This document explains how we manage Java versions locally and in CI, how SDKMAN is used to auto-switch toolchains in this repository, and how Kotlin and Java language levels are configured.

## JDK policy (brief)
- Baseline Kotlin target: Kotlin compiles with jvmTarget=21 for maximum compatibility and stability.
- Java language level: Java sources compile with language level 25 via maven-compiler-plugin `<release>25`.
- Local runtime: By default, developers use Java 25 Temurin via SDKMAN (repo-local .sdkmanrc). Kotlin code still targets 21.
- CI validation: GitHub Actions runs a matrix on Java 21 and Java 25 to catch regressions early.
- Upgrade process: When Kotlin gains official support for a newer jvmTarget (matching the next LTS), we can revisit this policy. Changes must update: this doc, `.sdkmanrc`, compiler plugin `<release>`, CI matrix, and any affected code/tests.

## Repo-local SDKMAN configuration
This repo contains a `.sdkmanrc` at the root that declares the toolchain for this project only:

```
java=25.0.1-tem
maven=3.9.9
```

A `.sdkmanrc` is repo-local. It does not change your global SDKMAN state.

### One-time setup
1) Ensure SDKMAN is installed and initialized in your shell.
2) Enable auto-env switching so SDKMAN reads `.sdkmanrc` when you `cd` into the repo:
```
echo "sdkman_auto_env=true" >> "$HOME/.sdkman/etc/config"
```
3) Open a new terminal (or re-source your shell init).

### Usage
- Enter the repo and install the declared versions if needed:
```
cd /path/to/cloudberries-candidate-match
sdk env install   # first time; installs missing versions
```
- On subsequent visits, just switch to the declared versions:
```
sdk env
```
- Temporarily override (e.g., to test on JDK 21 without changing `.sdkmanrc`):
```
sdk use java 21.0.7-tem
```
Leave and re-enter the repo (with auto-env enabled) to return to the `.sdkmanrc` versions.

## Maven compiler and Kotlin configuration
- Java sources compile with Java 25 language level:
  - maven-compiler-plugin `<release>25` is set in modules containing Java sources (e.g., `candidate-match`, `ai-rag-service`, `teknologi-barometer-service`).
- Kotlin remains at jvmTarget=21 (Kotlin 2.2 does not provide jvmTarget=25). This allows mixing new Java code with stable Kotlin bytecode.

## Testing on newer JDKs (MockK/Byte Buddy)
To support dynamic agents and module access on modern JVMs, we add explicit flags via Surefire (and Failsafe) and provide `.mvn/jvm.config` so IDE and CLI behave consistently:
- add-opens:
  - `--add-opens java.base/java.lang=ALL-UNNAMED`
  - `--add-opens java.base/java.lang.invoke=ALL-UNNAMED`
  - `--add-opens java.base/jdk.internal.reflect=ALL-UNNAMED`
- agent flags:
  - `-Djdk.attach.allowAttachSelf=true`
  - `-XX:+EnableDynamicAgentLoading`
- Test libraries pinned to recent versions:
  - MockK: `io.mockk:mockk-jvm:1.13.13` (test scope)
  - Byte Buddy: `net.bytebuddy:byte-buddy:1.15.10`, `byte-buddy-agent:1.15.10` (test scope)

This combination avoids typical agent/module issues on newer JVMs (post JEP-451) and stabilizes mocks across JDKs.

## CI strategy
- Build matrix runs with Java 21 and Java 25 (Temurin).
- The review/comment steps are independent of JDK and run after a successful build.
- This matrix protects against regressions on the current Kotlin baseline (21) and the latest LTS (25).

## Troubleshooting
- “Unsupported class version,” or IDE uses wrong JDK:
  - Ensure `sdk env` has been applied, or enable auto-env, or configure your IDE’s Maven runner to use Java 25 Temurin.
- MockK/Byte Buddy failures (agent/module warnings):
  - The surefire/failsafe `argLine` flags above must be present. Confirm `.mvn/jvm.config` is in effect for your IDE.
- Kotlin referencing Java 25-only APIs:
  - Mix is supported (Kotlin jvmTarget 21 + Java 25). If you observe ordering issues (Kotlin compiled before Java types are available), we can adjust compile order or add stubs. Report via PR.

## FAQ
- Is `.sdkmanrc` global?
  - No, it is repo-local. It only affects shells inside this repository (when auto-env is enabled or `sdk env` is run).
- Do I need both JDK 21 and 25 installed?
  - Not strictly, but CI validates both. Locally, we default to 25 via `.sdkmanrc`. Installing 21 can help debug matrix-only failures.
- How do I change the project’s JDK policy?
  - Open a PR that updates: this doc, `.sdkmanrc`, maven-compiler-plugin `<release>`, CI matrix, and any affected code/tests.
- Can I temporarily use a different JDK locally?
  - Yes: `sdk use java <version>`. Re-enter the repo to return to `.sdkmanrc`.

