# Alpha12: native smoothness and session fixes

Alpha11 made the feed script cheap. In a synthetic 150-post scroll benchmark it used about 110 ms of script time, against about 4.4 s for 0.1.1. The remaining lag and blank areas reported on the phone came from the Android side, so alpha12 changes the WebView host and leaves the alpha11 filter design intact.

## Rendering and navigation

- **The page is no longer hidden while it loads.** Alpha11 set the WebView to INVISIBLE on every page start and showed it only after `onPageFinished`, which waits for every image, so the screen stayed blank for seconds. Rules are now injected at `onPageCommitVisible`, when the page first draws, and again at `onPageFinished`. The injection is idempotent.
- **Tiles just outside the viewport are pre-rastered** (`setOffscreenPreRaster`), so fast flings show fewer blank areas.
- **Inbox/Feed switches no longer reload Instagram.** The page stays loaded and paused behind Ataraxia's own screens (INVISIBLE rather than GONE). Switching clicks Instagram's own link, falls back to a normal load if that link is missing or ignored, and re-applies the current mode to the live page.
- **Rotation, folding, keyboard and dark-mode changes keep the WebView** (`configChanges`), so these no longer trigger a reload or lose the scroll position.
- **Blocked routes are enforced on pushState** (`doUpdateVisitedHistory`) instead of waiting up to a second. A Reel opened from a DM steps back to the thread instead of reloading the inbox. A blocked link tapped on a page just cancels, and the page stays put.
- **Blocked Reels/Explore links are dimmed and disabled in place** (`data-quiet-inert`). Before, `display:none` left holes in Instagram's navigation bar and blank tiles in suggested-Reels rows.
- **The `; wv` / `Version/4.0` user-agent markers are dropped**, so Instagram serves its regular mobile site instead of its embedded-browser variant.
- A thin load indicator shows during page loads. It toggles between VISIBLE and INVISIBLE, so it never changes the layout.
- Clearing the Instagram login waits for cookie removal and always reloads, so the signed-out page is never reused.

## Session counting

- **A new calendar day resets session minutes as well as daily minutes.** Before, yesterday's leftover session time cut today's first session short. A break that's already running still applies across midnight.
- **A session ends after 30 minutes away from counted browsing** (`Budget.sessionStale`). Before, a session only ended after hitting the cap and sitting out the break, so short check-ins added up. A clock set backwards never ends a session early. Counters saved by alpha11 have no `lastActive` value and are treated as stale.

## Build

- The version lives only in `gradle.properties` (`ATARAXIA_VERSION_NAME` / `ATARAXIA_VERSION_CODE`). `app/build.gradle`, Android CI and `tools/build_sdk.py` read it from there. (The SDK script had been left at alpha03.)
- Persistent signing is unchanged: the manual `signed-build.yml` workflow is still fail-closed.

## Evidence and limits

- All existing contracts still pass: privacy, philosophy and UI source checks, policy/budget/backup JVM tests, source regression checks, browser fixtures, and all 12 feed-stability scenarios.
- New JVM assertions cover the idle-gap rule, clock skew, legacy counters and the user-agent rewrite. The browser fixtures now check that blocked links keep their layout slot and can't be tapped.
- The activity was compiled against Android API stubs in a build environment without the SDK. The CI Gradle build is the authoritative compile.
- Not verified on a phone: the fling feel, Instagram's response to the new user agent, soft Inbox/Feed switching against live markup, and login/DMs. Run the phone checks before calling it done.
