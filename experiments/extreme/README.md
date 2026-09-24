# Extreme mode — parked

The post counter remains a planned opt-in feature for Extreme mode. It is not
enabled or packaged in the current app. Focused and Balanced browsing retain
native time limits, with no post quota, identifier collection or count polling.

`filter-alpha10.js` preserves the previous prototype for later investigation;
it is not a validated implementation for release. Its historical browser tests
and native integration are in commit `72f9f585751228bb9922ad06f1287f169c84c6ae`.

Before reintroducing it, reproduce live-device failures, separate counting from
layout/filtering, validate fast swipes and recycled cards, and verify it adds no
feed jumps. It must be optional and must not run in Focused or Balanced mode.
