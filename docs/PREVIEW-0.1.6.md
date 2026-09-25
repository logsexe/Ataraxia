# 0.1.6 preview: portable settings

Settings now offers Export settings and Import settings through Android's document picker. No new manifest permissions are needed. Blocking document I/O runs off the UI thread.

The versioned .properties file includes only Focused/Balanced mode, post/session/daily boundaries and up to 20 saved usernames. It contains no Instagram cookies, credentials, messages, browsing history, post IDs, timers or cooldown. The file is not encrypted; a user-selected cloud document provider can upload it.

Import is bounded to 16 KiB, accepts only the exact supported keys and values, rejects duplicate fields and malformed/duplicate profiles, and validates the full file before asking the user to replace settings. Cancel or invalid input leaves preferences unchanged. Successful import preserves existing login and usage counters/cooldown; changing boundaries can of course change when those counters reach the configured limit.

## Phone verification

1. Set distinct boundaries, switch mode and save two profiles.
2. Export settings to a local file; inspect it for the expected fields and absence of account credentials.
3. Change settings and saved profiles, then import. Check the preview and cancel: nothing should change.
4. Import again and confirm. Check mode, limits and saved profiles; login and current usage counters should remain unchanged.
5. Try a malformed file, oversized file and cancelled picker. Confirm a useful error or unchanged state.
6. Keep a backup off-device before a later reinstall. Reinstall itself clears app login/settings/counters; restoring preferences does not restore the old usage counters or Instagram session.

This export capability begins with 0.1.6: older installed builds cannot export settings using it before upgrading. CI exercises round trips and malicious inputs, plus existing policy/filter/signing checks. Android document-provider interaction still needs phone testing. Persistent signing is prepared but not provisioned, so this preview still needs reinstalling over differently signed previews.
