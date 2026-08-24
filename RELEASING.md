# Releasing

Platypus ships to GitHub Releases (APK + AAB), the Play Store (AAB), and the App Store (iOS).
Android release artifacts are built and attached to a GitHub Release automatically on tag; the
Play and App Store uploads are manual.

## Signing

One keystore signs every Android build: debug, release, and the GitHub artifacts. Keep it and its
passwords out of the repo. The Gradle build reads them from these properties or environment
variables (env wins in CI):

- `PLATYPUS_KEYSTORE_FILE`
- `PLATYPUS_KEYSTORE_PASSWORD`
- `PLATYPUS_KEY_ALIAS`
- `PLATYPUS_KEY_PASSWORD`

When no keystore is configured, debug builds fall back to the standard Android debug keystore and
`assembleRelease` is debug-signed, so contributors can build without secrets.

Play uses Play App Signing: the uploaded AAB is signed with this keystore (the upload key) and
Google re-signs it with the app signing key it manages. APKs distributed on GitHub are signed with
the upload key, so their signature differs from the Play-installed build; treat GitHub and Play as
separate install channels.

## Versioning

Bump both platforms before tagging:

- Android: `versionName` (and `versionCode`) in `androidApp/build.gradle.kts`.
- iOS: `MARKETING_VERSION` (and `CURRENT_PROJECT_VERSION`) in `iosApp/Configuration/Config.xcconfig`.

## Android: GitHub Release (automated)

1. Add these repository secrets (Settings > Secrets and variables > Actions):
   - `PLATYPUS_KEYSTORE_BASE64` — the keystore, base64-encoded (`base64 -i platypus.keystore | pbcopy`).
   - `PLATYPUS_KEYSTORE_PASSWORD`, `PLATYPUS_KEY_ALIAS`, `PLATYPUS_KEY_PASSWORD`.
2. Tag and push: `git tag v0.0.1 && git push origin v0.0.1`.
3. The Release workflow builds a signed APK and AAB and attaches them to the GitHub Release for the tag.

Local build (optional):

```
./gradlew :androidApp:assembleRelease :androidApp:bundleRelease \
  -PPLATYPUS_KEYSTORE_FILE=/path/platypus.keystore \
  -PPLATYPUS_KEYSTORE_PASSWORD=... -PPLATYPUS_KEY_ALIAS=... -PPLATYPUS_KEY_PASSWORD=...
```

Outputs: `androidApp/build/outputs/apk/release/*.apk`, `androidApp/build/outputs/bundle/release/*.aab`.

Smoke-test the release APK on a device before submitting; it is R8-minified and only a device run
proves the keep rules are complete.

## Play Store (manual)

1. Download the AAB from the GitHub Release.
2. In Play Console, create the app, enroll in Play App Signing, and upload the AAB to a track.
3. Complete the store listing, content rating, and data-safety form (no analytics or tracking; data
   stays on device), then roll out.

## iOS: App Store (manual)

1. In Xcode, set your Apple Developer team and signing for the `iosApp` target.
2. Product > Archive, then distribute through the Organizer to App Store Connect.
3. Submit for TestFlight or App Store review.

iOS is distributed through the App Store only; GitHub Releases carry the Android artifacts.
