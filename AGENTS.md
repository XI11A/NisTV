# NisTV Revamped — Agent Engineering Rules

These rules govern all AI coding agents working within the NisTV Revamped codebase. Every agent must adhere strictly to these direct instructions.

## Versioning System Rules

1. **Check Current Version First**:
   Before starting any task, read `version.properties` and note the current version.

2. **Determine Bump Type Upon Completion**:
   After finishing a task that changes the project, decide the bump type using these rules:
   - **MAJOR (`X.0.0`)**: Breaking or incompatible changes:
     - Room schema changes that need a destructive migration or change data meaning
     - Removing or renaming existing features, screens, or routes
     - Changing the download file location or format
     - Changing auth / login flow
     - Minimum SDK (`minSdk`) or Target SDK (`targetSdk`) change
     - Replacing a core library (ExoPlayer, Room, Compose Navigation)
     - Any change that can lose user data
   - **MINOR (`x.Y.0`)**: New backward-compatible functionality:
     - New feature, screen, route, setting, provider, or entity with a safe additive migration
     - Meaningful UI redesign of a screen
     - New background worker
     - New external API integration
   - **PATCH (`x.y.Z`)**: Bug fixes and maintenance:
     - Bug fixes
     - Small UI/text tweaks
     - Performance or stability improvements
     - Dependency patch updates
     - Refactors with no behaviour change
     - Test-only or docs-only changes (*Note: docs/test-only changes may skip the bump; state that explicitly in the final reply*)

3. **Highest Level Precedence**:
   If one task contains several kinds of change, use the **HIGHEST** level. Never bump more than one level per task. Never lower or reset a version except through the major/minor reset rule.

4. **Automated Bump Execution**:
   Apply the bump **ONLY** by running:
   ```bash
   ./gradlew bumpVersion -PbumpType=<major|minor|patch> -Pmessage="<what changed>"
   ```
   **Never** hand-edit `version.properties` or the `versionCode`/`versionName` in `app/build.gradle.kts`.

5. **Room Schema & Migration Verification**:
   If the change touches the Room schema, also confirm that:
   - The migration exists
   - The DB version in `NisTvDatabase.kt` was incremented
   - A migration test was added or updated
   *Note*: A schema version bump alone does not decide the app version: a safe additive migration is **MINOR**, while a data-changing or destructive migration is **MAJOR**.

6. **Changelog Maintenance**:
   Add the change to `CHANGELOG.md` under the new version heading (`Added`, `Changed`, `Fixed`, `Removed`, `Security`) in the same commit.

7. **Project Specification Synchronization**:
   Update the header of `PROJECT_SPEC.md` (current version, date) and any section the change affects.

8. **Mandatory Final Reply Version Statement**:
   The final reply of every task must end with:
   ```text
   Version: <old> -> <new> (<major|minor|patch|none>) - <reason>
   ```

9. **Conservative Level Selection**:
   If unsure between two levels, choose the higher one and explain why.

10. **Immutable App Identity**:
    Never change the `applicationId`, signing config, or package name as part of a version bump.

## Release & Distribution Workflow (Mandatory on Every Change)

Whenever a task modifies the app or project (excluding docs/test-only tasks that skip a version bump):

1. **In-App Changelog**:
   Update `APP_CHANGELOG` in `app/src/main/java/com/nis/nistv/core/designsystem/component/ChangelogDialog.kt` with the new version entry (marked `tag = "Latest"`, and demote the previous `"Latest"` entry to `"Patch"` / `"Minor"` / `"Major"`).
2. **Release APK Build**:
   Run `./gradlew assembleRelease` to build the signed release APK (`app/build/outputs/apk/release/app-release.apk`).
3. **Website APK & Metadata Update**:
   - Copy the built release APK to `website/public/downloads/nistv-v<versionName>.apk` and `website/public/downloads/nistv-latest.apk`.
   - Compute exact `fileSizeBytes`, `apkSize` (e.g. `"15.2 MB"`), `sha256`, and `md5` from the built APK.
   - Update `website/public/version.json`, `website/public/api/version.json`, and `website/src/data/appData.ts` (`CURRENT_RELEASE` and `CHANGELOG_DATA`).
   - Run `cd website && npm run build` to verify the website TypeScript and Vite production build passes.
4. **Git Commit & Push**:
   Stage all changes (`git add -A`), commit with a clear message referencing the new version, and push to `origin main` (`git push origin main`).
