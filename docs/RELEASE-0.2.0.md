# Still 0.2.0

The screen is Still. The package id remains `app.ataraxia.social`.

- Paper, Fraunces, and Outfit replace the dark green pilot.
- First open explains that sign-in is Instagram’s own page. Still has no password form.
- Home offers the inbox or a short browse, and shows the session limits. Inbox time is not counted.
- Reels, Explore, Shop, and Live routes bounce home. A profile live route does too. Sponsored posts are hidden when the label can be seen.
- Enough for now leaves the inbox open and closes browsing.
- Settings changes the three limits and can clear the login stored in the app.
- `web/` is a practice with separate Friends and Known rooms. It does not embed instagram.com.

## Installation

Download Still-0.2.0.apk and open it on Android 11 or newer. A build signed with a different key will not update an older install in place. Uninstalling clears the login and the local counters, not the Instagram account.

This CI build is signed with a test key and debugging is disabled. It is not a production signature.

## Validation and limits

Publication is gated on the Java policy and budget tests, the practice-room checks, the DOM regressions, the Chromium filter fixtures, and the APK manifest check. It has not been tested here on a physical phone or against a logged-in Instagram session.

Shop and Live blocking is path-based, the same kind of rule as Reels and Explore. Ad filtering remains best effort. Limits apply only inside Still. Meta still sees the activity.
