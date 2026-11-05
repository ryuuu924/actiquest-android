# Contributing to ActiQuest

Thank you for your interest in contributing to ActiQuest! ActiQuest is an open-source, gamified campus fitness and verification platform for universities, built natively in Kotlin and Jetpack Compose.

---

## 1. Code of Conduct

We expect all contributors to adhere to a respectful, welcoming, and inclusive standard of communication:
- Be respectful and considerate of other contributors, student developers, and university reviewers.
- Focus on constructive feedback regarding code quality, UI responsiveness, and security.

---

## 2. Development Setup

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or newer.
- **JDK Version**: Java 17 (Temurin or FooJay resolver convention).
- **Android SDK**: API 35 (Android 15) compile SDK, min SDK API 24 (Android 7.0 Nougat).
- **Core Library Desugaring**: `com.android.tools:desugar_jdk_libs:2.0.4` (enabled for `java.time` support).

### Initializing Project
1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_GITHUB_USERNAME/actiquest-android.git
   cd actiquest-android
   ```
2. Copy `google-services.json.template` to `app/google-services.json` and configure your Firebase project credentials.
3. Open the project in Android Studio and run a Gradle Sync.
4. Execute unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 3. How to Submit Contributions

### Branching Strategy
- `main`: Stable, production-ready release branch.
- `dev`: Active integration branch.
- `feature/*`: Specific feature branches (e.g., `feature/new-badge-rule`).
- `fix/*`: Specific bugfix branches.

### Pull Request Workflow
1. Fork the repository on GitHub.
2. Create a feature or bugfix branch: `git checkout -b feature/my-cool-feature`.
3. Commit your changes with clear, descriptive commit messages.
4. Ensure all unit tests pass locally (`./gradlew testDebugUnitTest`).
5. Push to your fork and submit a Pull Request targeting the `dev` branch.
6. Provide a detailed summary in the PR template explaining the changes and testing performed.

---

## 4. Coding Standards

- **Kotlin Best Practices**: Follow standard Kotlin style guidelines and Compose idiomatic patterns.
- **UI & Accessibility**:
  - Always support **Light Mode** and **Dark Mode** seamlessly via `ThemeManager.isDarkMode`.
  - Use `LightTextPrimary` / `LightTextSecondary` for high contrast on light surfaces.
  - Avoid hardcoding dark gray colors on light background containers.
- **Timezone Integrity**:
  - Use `ZoneId.of("Asia/Manila")` for all calendar representations, streaks, and timestamps.
- **Security Guardrails**:
  - Never commit private keys, `.jks` keystores, or `local.properties` to version control.
