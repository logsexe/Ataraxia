# Alpha11 — scrolling first, Extreme mode parked

The counter remains a planned feature. Alpha11 removes it from normal browsing:
no post-count UI, post quota, native snapshot polling, identity hashing, viewport
geometry engine or ResizeObserver. The prototype is preserved under
`experiments/extreme/` and is explicitly checked not to ship in the APK. Old backup
post-limit values remain readable and retained, but cannot affect time limits.

Active filters now defer DOM scans and layout writes while a pointer is held or
scroll events continue. After 160 ms without motion, pending changes are batched
once. Caption edits and style/class animations do not trigger metadata rescans.
Exclusive ad wrappers still collapse; surviving content uses native anchoring.
There are no custom scroll-position writes. Newly discovered ads can remain
visible until scrolling settles; stability takes priority over mid-fling hiding.
Collapsed cards retain their media boundary: percentage image widths no longer
cause an already filtered ad to reappear when its metadata updates.

The header reserves stable space for the timer. Focused/Balanced modes, native
time limits, Inbox exemptions, route blocking, uploads and settings backup stay.
Extreme mode is labelled planned, with no nonfunctional toggle. Following-only,
new-content and friends-first ideas are tracked in `ROADMAP.md`.

## Evidence and limits

The three gesture/inertia regressions in `tests/feed-stability.test.cjs` fail
against the preserved alpha10 filter: it collapses a late ad during an active
drag or fling. They pass with alpha11, including window and nested anchors.
The suite also covers whole-wrapper collapse, no scroll-time DOM work, caption
animation, incremental discovery, recycling, supported non-semantic cards,
inbox isolation and the inability of old quotas to activate a feed cap.

The browser fixtures, native policy/budget/backup checks and Android CI validate
controlled cases. They do not verify authenticated Instagram on the user's
phone, all current markup, physical fling behaviour or every ad. Do not call
scrolling finalised until the phone checks pass.

The branch CI APK is non-debuggable and test-signed. A different signing key
requires uninstall/reinstall; export settings first, since uninstalling clears
app data and Instagram login. Persistent release signing remains protected.
