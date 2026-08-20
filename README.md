# Platypus

An unofficial Bitbucket Cloud client for Android and iOS, built with Kotlin Multiplatform.

Platypus talks to Bitbucket Cloud directly from the device. Repository content never
passes through a Platypus server. It shares one Compose Multiplatform codebase across
Android and iOS.

> Early development. The app shell and theme are in place; features land step by step.

## Building

Requires JDK 21.

```
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
open iosApp/iosApp.xcodeproj
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

```
Copyright 2026 Joel Kanyi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
