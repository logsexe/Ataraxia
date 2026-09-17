# Validation — 17 September 2026

## Completed before the Ataraxia rename

- The same Android Java implementation compiled as the Quiet Social prototype with JDK 17 against official Android SDK 35. The Ataraxia rename changes package and product identifiers without changing policy logic.
- D8 converted compiled bytecode for minimum API 30.
- AAPT2 packaged manifest, icon and bundled rules. APK aligned and signed with a pilot RSA key.
- APK signature verified using Android apksigner (v3).
- Prototype packaged manifest inspected: minimum SDK 30, target SDK 35, version `0.1.0-pilot`, Internet was the ONLY requested permission. No debuggable flag.
- 25 navigation/security policy assertions passed on the JVM.
- 9 native budget boundary assertions passed on the JVM.
- JavaScript syntax check passed.
- Playwright browser fixture tests passed in Chromium: Reels/Explore link hiding; dynamic route-link hiding; sponsored label filtering; preserving ordinary captions about advertising; unique post counting; repeated injection; not inspecting/filtering inbox content; Afrikaans sponsored labels; refusing look-alike origins.

## Not verified

- The renamed `app.ataraxia.social` build must pass the repository's Android CI workflow before a signed Ataraxia APK is published.

- No Android emulator or physical device launch was performed. Device UI/rendering, lifecycle/persistence integration and installation must be checked on a phone.
- No real Instagram account was used. Login/2FA, live feed markup, DM compatibility, selected-media uploads, actual sponsored-content coverage and GrapheneOS/Proton VPN compatibility remain unverified.
- The standard Gradle Android plugin could not be resolved in this environment. The supplied APK was built using the official Android SDK tools directly (`tools/build_sdk.py`). Gradle wrapper generation succeeded; Android Studio/Gradle build files are included, but that build route has not completed here.
- No independent security audit, Play Store review or comprehensive ad-blocking claim.

See PHONE-TEST-CHECKLIST.md before relying on the app or removing the official client.
