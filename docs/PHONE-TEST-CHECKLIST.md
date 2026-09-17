# First phone test

Do not remove your existing Instagram app until these pass. Test in the intended GrapheneOS profile. This pilot's notification forwarding is irrelevant because it has no push notifications.

- [ ] Install the supplied APK on Android 11+; verify it asks for no privileged permissions.
- [ ] Open Home. Verify text and navigation at default and large font sizes, portrait and landscape.
- [ ] Open Inbox. Log in using Instagram username/password and complete 2FA.
- [ ] Confirm account identity and existing DM conversations; send a non-sensitive test message.
- [ ] Verify Reels and Explore links are hidden. Try a Reel sent in a DM: navigation must be stopped.
- [ ] Open Feed. Set five posts/two minutes for testing. Check native counters.
- [ ] When a cap is reached, Inbox remains accessible but Feed is stopped.
- [ ] Close/reopen the app: it must not reset the session/cooldown or daily allowance.
- [ ] Leave app in background: the active-use timer should stop. Resume and check it continues.
- [ ] Let the cooldown expire, then open Feed: session counters reset, daily time remains.
- [ ] Review a feed containing an ad. Report missed sponsored posts with sensitive data redacted. No claim of perfect blocking.
- [ ] Upload a selected image/video using the system picker; check that no broad storage permission is requested.
- [ ] Verify Settings > Clear Instagram login signs you out after navigating again.
- [ ] Test with Proton VPN connected.

Known fallback: if Instagram uses unsupported feed markup, post counting/filtering may not work. Native time limits should still work. Login, messaging, upload or web layout failures may require changes to the pilot. If a critical flow fails, keep the official app while reporting the issue.
