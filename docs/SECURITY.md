# Security notes

This is an unaudited pilot. No app is a safe place for credentials solely because its source is available.

- Internet is the only declared permission. No Services, exported data providers, receivers or deep links. Only the launcher Activity is exported.
- No JavaScript-to-Java bridge. Native code reads aggregate filter status only when requested through the app menu. There is no background post-identifier polling and no privileged commands from webpage JavaScript.
- Exact Instagram hostname + HTTPS + standard-port allowlist for main-frame navigation; external HTTP(S) links require confirmation, other URI schemes are rejected. Policy tests include look-alike hosts, userinfo tricks, alternate ports and encoded blocked paths.
- JavaScript and DOM storage are needed for Instagram. Cookies stay in WebView's private app data. Third-party cookies, cleartext traffic, mixed content, file/content access, pop-ups, microphone/camera web permissions and geolocation are disabled. TLS errors are cancelled, not overridden.
- Android cloud backup and WebView debugging are disabled. The supplied APK has no debuggable manifest flag. No developer server, crash reporter, analytics SDK or remote code-update channel.
- Native time accounting is independent of webpage scripts. Website changes or malicious page scripts can evade cosmetic rules, but cannot directly reset the native time allowance. Post counting is parked outside the APK for future Extreme mode.
- Meta still receives account and usage data. Subresources are not restricted to the main-frame allowlist because Instagram uses CDNs. The tiny ad-host blocklist is not comprehensive tracking protection. WebView Safe Browsing/provider behavior is platform-dependent.
- Voluntary limits: the device owner can change settings or clock, clear app storage, switch profiles/apps or uninstall. No tamper-proof or whole-phone enforcement is claimed.
- Source changes require a newly signed APK. Preserve a private release key if maintaining this app. Never place signing keys, session cookies or passwords in a public repository.

Before a public release: independently review login/session handling, test on real Pixel/GrapheneOS devices, add instrumentation coverage, establish a maintained signing/update process, validate accessibility, and test current live Instagram markup. UI filtering is a continuing maintenance responsibility.
