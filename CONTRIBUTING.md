# Contributing

Thanks for your interest in Platypus.

## Building

Requires JDK 21.

```
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
open iosApp/iosApp.xcodeproj
```

## Code style

Formatting is enforced with Spotless and ktlint.

```
./gradlew spotlessApply   # format
./gradlew spotlessCheck   # verify, runs in CI
```

Keep changes focused. Do not reformat unrelated code in the same pull request.

## Pull requests

- Branch off `main`, one topic per pull request.
- Make sure the build, the tests, and `spotlessCheck` pass.
- Never commit secrets, tokens, or keystores.
