# Still 0.2.1

The screen is Still. The package id remains `app.ataraxia.social`.

0.2.0 closed as soon as it opened. It asked for the system bars before the window existed. This build waits until the window is up.

- Paper, Fraunces, and Outfit replace the dark green pilot.
- First open explains that sign-in is Instagram’s own page. Still has no password form.
- Home offers the inbox or a short browse, and shows the session limits. Inbox time is not counted.
- Reels, Explore, Shop, and Live routes bounce home. A profile live route does too. Sponsored posts are hidden when the label can be seen.
- Enough for now leaves the inbox open and closes browsing.
- Settings changes the three limits and can clear the login stored in the app.
- `web/` is a practice with separate Friends and Known rooms. It does not embed instagram.com.
- If the system browser component is missing, Still stays open and says so, instead of closing.

## Installation

Download Still-0.2.1.apk and open it on Android 11 or newer. Uninstall Still 0.2.0 first. This build is signed with a new test key, so it cannot update 0.2.0 in place. Uninstalling clears the login and the local counters, not the Instagram account.

This CI build is signed with a test key and debugging is disabled. It is not a production signature.

## Validation and limits

Publication is gated on the Java policy and budget tests, the practice-room checks, the DOM regressions, the Chromium filter fixtures, and the APK manifest check. The launch crash was identified from the window setup order. It has not been retested here on a physical phone or against a logged-in Instagram session.

Shop and Live blocking is path-based, the same kind of rule as Reels and Explore. Ad filtering remains best effort. Limits apply only inside Still. Meta still sees the activity.
