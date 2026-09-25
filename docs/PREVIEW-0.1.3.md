# 0.1.3 preview — reliability and everyday navigation

This continues the unmerged stability branch. It is not a production release.

- Back follows permitted WebView history; exhausted browsing budgets still block feed history while allowing inbox history.
- Settings includes Refresh current page and Filter status. Status displays aggregate container/hidden-item counts, not account or message content. Detection is best effort; recognised containers do not guarantee recognised post links.
- The status line shows the remaining break countdown and daily exhaustion explicitly.
- Expired breaks reset both native and live webpage session state, including when returning from Inbox without a reload. Old asynchronous snapshots cannot restore the previous session's counts.
- Unchanged post sets are no longer written to preferences each second.
- Ad metadata detection supports div labels and CSS-sized avatars.
- Nested scroll containers use their visible boundaries for counting, while posts crossed in fast swipes still count.

## Acceptance checks on the phone

1. Open feed and check Settings → Filter status. Compare the hidden count with ads observed; report any misses.
2. Try slow and fast swipes. Set a five-post cap and confirm it stops browsing.
3. Visit Inbox during the break, wait until it expires, then return using Instagram navigation. Confirm a new session starts and old counts do not return.
4. Open a profile/post and press Back. Confirm it returns to the previous permitted page.
5. Refresh from Settings. Confirm login, allowance and session counts persist.
6. Confirm messages remain available after the feed limit, and prohibited routes stay blocked.

Automated checks cover filter fixtures, session reset, nested scrolling, policy/budget boundaries, compilation, APK signature and Internet-only permissions. Real Instagram and Android interaction checks still need a phone. CI test signing is temporary: a new build can require reinstalling and signing in again. Persistent release signing remains outstanding before stable distribution.
