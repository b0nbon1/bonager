# Bonager

Bonager is a native Android productivity app written in Kotlin with Jetpack Compose.

It includes:

- task planning, priorities, due dates, and native reminders
- client work logs and unpaid totals
- a Pomodoro focus timer
- notes and a daily journal
- daily goals with streak tracking
- income, spending, and monthly summaries
- local SQLite storage in `bonager.db`

## Run

Open the `android` directory in Android Studio, or build from the command line:

```sh
cd android
./gradlew assembleDebug
```

The debug APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

Bonager uses the package ID `com.bonvic.bonager`, targets Android API 36, and supports Android 7.0 and newer.
