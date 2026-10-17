# Monthly Budget

A small, offline budget app for Android and iOS using Kotlin Compose Multiplatform.

One screen shows your recurring payments and a spending chart. Tap **+** to add a
payment; tap a payment to edit or delete it. Rent starts at **$220**, due on day
**25**, and Cellcard SIM at **$6**, for a monthly total of **$226**.

## Run

Use JDK 17 or 21. Gradle is included through the standard wrapper.

For Xcode, install a JDK registered with macOS (check with
`/usr/libexec/java_home -V`). The build phase selects JDK 17, or JDK 21 if 17 is
unavailable, instead of using an unsupported system default such as Java 25.
If neither is installed, install [Temurin 17](https://adoptium.net/temurin/releases/?version=17)
and restart the build. Gradle failures also include their stack traces.

- **Android:** open the project in Android Studio, install Android SDK 36, and run
  `composeApp` on an emulator or device with Android 7.0+.
- **iOS:** on a Mac with Xcode 16+, open `iosApp/iosApp.xcodeproj`, choose the
  `MonthlyBudget` scheme and an iOS 15+ simulator, then Run. Select your own
  signing team to run on a physical iPhone. Xcode must have access to your JDK
  (`JAVA_HOME`); its build phase compiles the shared Kotlin framework.

```sh
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug
```

GitHub Actions builds Android and iOS. The Android job also runs the shared
domain/storage tests and uploads a debug APK.

## Design

- Shared Material 3 UI, system light/dark theme, scrolling content and safe insets.
- A Compose Canvas draws the chart; no charting or dependency injection library.
- Amounts use integer cents, so calculations and storage preserve exact values.
- JSON records live in app-private SharedPreferences on Android and UserDefaults
  on iOS. No account, server, internet permission, analytics, or cross-device sync.
- Saving happens off the main thread. A failed save keeps the editor open, and
  unreadable saved data is retained rather than replaced with starter payments.
- Due days describe the recurring day of the month; they do not schedule reminders
  or calculate overdue payments. Everything is in USD.

The app seeds the two starter payments only when no saved budget exists. Deleting
all payments leaves an empty budget. Uninstalling the app removes its local data.

## Preview and verification

The image in [docs/preview.png](docs/preview.png) is a **design mockup**, not a
screenshot of a running app. The shared UI implements that simple layout with
payment entry added. Native compilation still needs to be checked by the build
workflow or a machine with the platform SDKs; no native build is claimed here.
