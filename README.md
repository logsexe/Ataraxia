# Ataraxia

> Social media without the noise. Connect intentionally. Leave peacefully.

[![Android CI](https://github.com/logsexe/Ataraxia/actions/workflows/android.yml/badge.svg)](https://github.com/logsexe/Ataraxia/actions/workflows/android.yml)

An Instagram web client designed to make checking in finite. Free, MIT-licensed source. No developer backend, ads, analytics, subscription, device-admin, Accessibility, overlay, VPN or usage-access permissions. Not affiliated with Instagram or Meta.

## Install the pilot

1. Build the pilot from source with Android Studio or download the test-signed APK from [Releases](https://github.com/logsexe/Ataraxia/releases). See [v0.1.1 installation and signing notes](docs/RELEASE-0.1.1.md).
2. Allow installation from the browser/files app only when needed, install, and turn that installation permission back off afterwards. If your Owner policy disables installation in secondary users, install via Owner and use its “Install available apps” control instead.
3. Open Ataraxia, read the pilot notice, and choose **Open Instagram inbox**.
4. Log in on Instagram's own webpage with your usual username/password and 2FA. Facebook sign-in/pop-ups are not supported. The app does not provide a separate credential form.
5. Try the inbox, open Feed, then test the shortest limits from Settings. Verify the feed is stopped and inbox remains accessible.

This is a pilot, not a production release or independently audited app. Keep the official app until the pilot passes your tests. Builds made with different signing keys cannot update one another without uninstalling first. Uninstalling clears the login and local counters, not your Instagram account.

## What it does

- Inbox-first home screen and native navigation.
- Blocks Instagram `/reel`, `/reels`, `/explore` and `/tv` routes; hides matching links inside the website.
- Defaults: 10 identified feed posts, 5 browsing minutes per session, 15 browsing minutes per day, 10-minute cooldown after a capped session.
- Counters persist locally across app restarts. A local calendar-day change resets the daily time allowance. Login and direct-message routes remain exempt.
- Best-effort filtering of recognised Sponsored/suggested labels in feed metadata (English and Afrikaans), plus a small explicit advertising-host blocklist.
- Allows user-selected image/video uploads through Android's system document picker. No broad photo-library permission.
- Settings can clear the app's Instagram cookies, website storage and cache.

## Honest limits

- This only changes Instagram inside Ataraxia. It cannot modify the official Instagram app or stop you using other browsers, changing settings/time, clearing data or uninstalling it.
- The website still selects content. This does not replace Meta's recommendation algorithm or guarantee a chronological/following-only feed.
- Ad/suggestion detection and post counting depend on website markup. Some feeds may not expose supported article/permalink structures. Native time limits remain independent of that markup. No promise of zero ads or tracking.
- SPA route enforcement includes a native one-second check; unusual navigation may briefly render before being blocked. Ordinary blocked links are intercepted directly.
- An identified post is counted when it enters or is crossed by the visible feed area, not after a proven reading time. Large viewports can show more than one at once. No cap is applied to messages.
- No native calls, push/background notifications, downloads, camera capture or microphone support in this pilot. User-selected uploads need device/live-site testing.
- Instagram may reject embedded browsers or change login, messaging, verification or feed layouts. Login/DM compatibility with a real account has NOT been tested here.
- The app needs network connectivity and a current Android System WebView/GrapheneOS WebView. Internet is its only manifest permission. No Google Play Services SDK is bundled.
- Safe Browsing is enabled; provider behavior depends on your system WebView. Meta and its loaded resources still receive network traffic and account activity. This is not an anonymous or fully de-Googled social service.

## Build on Windows

Install Android Studio with JDK 17 support and Android SDK platform 35. Open this directory as a project, let Gradle sync, and use Build > Build APK(s), or run:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK output: `app\build\outputs\apk\debug\app-debug.apk`.

This project uses Gradle 8.9 / Android Gradle Plugin 8.7.3, Java 17, compile/target SDK 35, minimum Android 11 (API 30). No third-party runtime dependencies. The debug variant is explicitly non-debuggable. Android Studio can create a release signing key for your own maintained distribution.

If Gradle cannot locate the SDK, let Android Studio generate `local.properties` or set `ANDROID_HOME`. Do not commit `local.properties` or signing keys.

## Tests

Pure Java policies, no Android runtime required:

```sh
mkdir -p build/tests
javac -d build/tests app/src/main/java/app/ataraxia/social/Policy.java app/src/main/java/app/ataraxia/social/Budget.java tests/PolicyTest.java tests/BudgetTest.java
java -cp build/tests PolicyTest
java -cp build/tests BudgetTest
```

Browser fixtures: install Node.js and Playwright in a test environment (`npm install --no-save playwright` and `npx playwright install chromium`), then run `node tests/filter.test.cjs`. These are synthetic DOM fixtures, not evidence of live Instagram compatibility.

See `docs/VALIDATION.md` for build evidence and the remaining phone checklist.
