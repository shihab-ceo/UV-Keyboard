# UV Keyboard

UV Keyboard is an Android input method with Bengali and English layouts, suggestions, clipboard history, voice input, and translation.

## Open and run

1. Open this folder in Android Studio and allow Gradle sync to finish.
2. Create or download the Gradle wrapper for the Gradle version in `gradle/wrapper/gradle-wrapper.properties` if your checkout does not include `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar`.
3. Add the Firebase `google-services.json` for your own Firebase project to `app/` if you use Firebase-backed features.
4. Run the `app` configuration on an Android device, then enable UV Keyboard under **Settings → System → Keyboard → On-screen keyboard**.

The repository currently does not include the Gradle wrapper scripts/JAR or Firebase app configuration. Firebase-dependent features cannot work until a project configuration is supplied.

## Features

- Jatiya, Avro, Bijoy, Prabhat, and English layouts
- Optional number row and swipe typing
- Personal word suggestions and local clipboard history
- Translation and voice input

## Privacy

Android Auto Backup and device-transfer backup are disabled for app data. Clipboard history and learned words remain local to the device; cloud sync is not configured in this checkout.
