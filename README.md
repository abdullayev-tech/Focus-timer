# Focus Timer

A quiet Android timer for one thing at a time, built with Jetpack Compose and Material 3.

No account, no ads and no tracking. The app has no internet permission.

## Build

JDK 21 and the Android SDK.

```bash
./gradlew assembleRelease   # unsigned unless a key is set, see below
./gradlew assembleDebug     # needs debug.keystore in the repo root
./gradlew testDebugUnitTest
```

To sign a release, set `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD`. The key alias is `upload`.

Every push to `main` also builds a debug APK in GitHub Actions: open the run under **Actions** and download **FocusTimer-APK**.

## F-Droid

The store text, icon and screenshots are in [fastlane/metadata/android/en-US](fastlane/metadata/android/en-US). F-Droid reads them from the repo. The build recipe is [.fdroid.yml](.fdroid.yml).

To submit:

1. Tag the release and push the tag: `git tag v1.0 && git push origin v1.0`.
2. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata) on GitLab.
3. Copy `.fdroid.yml` to `metadata/io.github.abdullayev_tech.focustimer.yml` on a new branch of the fork.
4. Open a merge request and fill in its template.

For each later release, raise `versionCode` and `versionName` in [app/build.gradle.kts](app/build.gradle.kts), add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, and push a `v<versionName>` tag. F-Droid picks up new tags by itself.

## License

[MIT](LICENSE)
