# Ataraxia priorities

## Now: a stable basic feed

- Smooth scrolling without count polling or filter changes during a drag/fling.
- Remove recognised ads/suggestions and their spacing wrappers after motion settles.
- Keep Inbox, Focused/Balanced modes, session/daily time limits and local settings.
- Validate the user's real feed on Android before calling scrolling finalised.

## Next: connections and new content

- Following-only feed: prefer Instagram's own supported Following view where
  available, with a clear indication of the selected feed. Confirm whether
  "followers" means accounts the user follows, followers, or mutual connections
  before implementation. Do not claim the current home feed is following-only.
- New content only: investigate a local read marker that survives reloads and
  recycled cards without depending on scroll counting. Define what "new" means
  before enabling it, and offer an explicit way to revisit older posts.
- Friends first: use a user-chosen favourites/friends list or supported native
  favourites view. Never infer friendship from follower count or label accounts
  as celebrities/influencers automatically. Avoid reordering visible posts mid-scroll.
- No ads is the goal. Continue improving recognised ad removal using real feed
  samples; don't promise perfect blocking or infer zero Meta tracking from it.

## Later: optional Extreme mode

Post-count limits are parked, not abandoned. The alpha10 prototype is preserved
in `experiments/extreme/`, outside the app package. Saved post-boundary preferences
remain compatible with older settings backups but have no runtime effect.

Reintroduce counting only as explicit opt-in Extreme mode after fast swipes,
recycling, delayed metadata and device performance have been validated. Focused
and Balanced modes must never run it in the background.
