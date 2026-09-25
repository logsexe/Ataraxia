# First phone test

Do not remove your existing Instagram app until these pass. Test in the intended GrapheneOS profile. This pilot's notification forwarding is irrelevant because it has no push notifications.

- [ ] Install the supplied APK on Android 11+; verify it asks for no privileged permissions.
- [ ] Open Home. Verify text and navigation at default and large font sizes, portrait and landscape.
- [ ] Open Inbox. Log in using Instagram username/password and complete 2FA.
- [ ] Confirm account identity and existing DM conversations; send a non-sensitive test message.
- [ ] Verify Reels and Explore links are hidden. Try a Reel sent in a DM: navigation must be stopped.
- [ ] Open Feed. Set two minutes for the session. Check the remaining-time display; no post counter or post quota should appear.
- [ ] When a time limit is reached, Inbox remains accessible but Feed is stopped.
- [ ] Close/reopen the app: it must not reset the session/cooldown or daily allowance.
- [ ] Leave app in background: the active-use timer should stop. Resume and check it continues.
- [ ] Let the cooldown expire, then open Feed: session time resets, daily time remains.
- [ ] Swipe slowly and fling quickly in both directions. A late ad should not collapse while the feed is moving; after settling, check for leftover space and a backward jump.
- [ ] Check Settings: Extreme mode is labelled planned and cannot be enabled. Importing an older post boundary must not cap the feed.
- [ ] Review a feed containing an ad. Report missed sponsored posts with sensitive data redacted. No claim of perfect blocking.
- [ ] Upload a selected image/video using the system picker; check that no broad storage permission is requested.
- [ ] Verify Settings > Clear Instagram login signs you out after navigating again.
- [ ] Test with Proton VPN connected.

Known fallback: if Instagram uses unsupported feed markup, ad filtering may not work. Native time limits should still work. Login, messaging, upload or web layout failures may require changes to the pilot. If a critical flow fails, keep the official app while reporting the issue.

## Alpha12 additions

- [ ] Fling the Balanced feed fast in both directions: no blank screen while it loads, no blank tiles, no stutter beyond Instagram's own.
- [ ] Switch Inbox ↔ Feed from the bottom bar: the page changes without a full reload or white flash.
- [ ] Rotate the phone and open/close the keyboard in a DM: Instagram doesn't reload.
- [ ] Reels/Explore icons appear dimmed and can't be tapped, with no gap in Instagram's navigation bar. A Reel shared in a DM is stopped and you return to the thread.
- [ ] Log in again and open DMs with the new user agent: login, 2FA and messaging still work.
- [ ] Browse, stay away 30+ minutes, return: the session starts fresh and daily minutes are kept. After midnight, both reset.
- [ ] Settings → Clear Instagram login: you're signed out and see Instagram's login page, not a stale page.

