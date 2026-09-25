# Alpha10 feed stability

Alpha09's browser tests missed the reported defects. Eight new real Chromium
reproductions failed against its unchanged filter: padded/fixed outer slots,
window and nested scroll writes on late filtering, modern permalinks, linkless
cards, non-semantic cards, hidden organic Reel media, and DOM work during the
actual animation frame. All eight pass with alpha10.

## Changes

- Collapse the exclusive post slot, including its padding/min-height/spacer,
  while preserving shared parents and scrollports. Restore wrappers on reuse.
- Never write a scroll offset or override scroll behaviour. Native anchoring
  keeps the surviving post in place in both window and nested fixtures.
- Cache geometry after layout changes. Scroll frames and steady-state native
  snapshots do no DOM queries or bounding-rectangle reads. Process only added
  content and dirty metadata, with one coalesced/cancellable frame.
- Count canonical/username permalinks, data post IDs, conservative div-based
  cards and linkless media cards. Hash media paths locally without URL queries;
  persist opaque IDs only. Dedupe permalink hydration and carousel changes.
- Sweep the interval crossed by fast scrolls, applying every clipping ancestor;
  never count offscreen nested feeds or loading skeletons.
- Apply native settings before first counting; poll at 200 ms, update the
  displayed count immediately in the callback, recover missing filters and
  expire stuck requests. Ignore stale callbacks across pages and sessions.
- Reserve both summary lines so timer/count changes cannot resize the WebView.

## Verification

Run `node tests/filter-regression.test.cjs` for narrow source safety checks.
Install the CI-pinned Playwright 1.56.1 and Chromium, then run:

```sh
node tests/filter.test.cjs
node tests/feed-stability.test.cjs
```

The stability suite also covers recycling, shared-wrapper insertion, nested
clipping, skeleton hydration, carousel dedupe, linkless author changes, inbox
isolation, initial configuration, and incremental append processing.
Android CI also runs policy/budget/backup tests, privacy/source checks, the
release-signing fail-closed test, builds the APK and verifies its manifest,
signature and ZIP alignment.

## Limits (not a live-site certification)

These are controlled browser fixtures, not an authenticated Instagram session
or an Android-device performance capture. Instagram can change its markup;
unrecognised non-semantic layouts still need an actual sample to support.
Linkless cards with no stable media URL get document-local identities and may
be recounted after reload. If a virtualizer reuses a linkless card with the same
author and no timestamp/post ID, it cannot reliably be distinguished from a
carousel change. The independent native time limits remain active.

The branch CI artifact is non-debuggable but test-signed. A different test
certificate requires uninstall/reinstall; export settings first, and expect
to sign in again. Persistent release signing remains protected and unchanged.
