# Ataraxia v0.1.1-pilot

Test release for Android 11 and newer.

- Counts recognised posts crossed during a fast swipe, including crossings in nested scrolling feeds.
- Covers the feed when the post cap is reached, between native counter polls. Seeds the cap with saved session counts.
- Expands sponsored metadata matching to accessible labels, split labels and role=article containers, while excluding caption-only matches and inbox content.
- Keeps native session/daily timers, inbox access and Internet-only permissions.

## Installation

Download Ataraxia-0.1.1-pilot.apk below and open it on Android. This app uses app.ataraxia.social, so it installs alongside the original Quiet Social pilot (app.quietsocial). Sign in again; the original pilot's settings, login and counters do not transfer.

This pilot is signed with a CI-generated Android test key, with debugging disabled. It is not production release signing. If you already installed a different Ataraxia CI build, its signature may differ; Android will reject an in-place update. Future releases need a persistent private signing key for reliable upgrades. Do not assume update compatibility.

## Validation and limits

Publication is gated on Java policy/budget tests, mocked-DOM regressions, Chromium browser fixtures, APK compilation and signature/manifest verification. It has not been tested on a physical phone or against a real logged-in Instagram session.

Fast-scroll counting still depends on recognisable post containers/permalinks; posts removed by virtualisation before observation may be missed. Ad filtering remains best effort for recognised English/Afrikaans metadata; zero ads is not guaranteed. Limits apply inside Ataraxia only.
