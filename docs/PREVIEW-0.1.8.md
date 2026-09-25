# 0.1.8 preview: modern calm interface

This preview replaces the pilot presentation layer with a coherent dependency-free Android visual system. Filtering, limits, privacy controls and philosophy remain unchanged.

## Visual system

- Deep forest background with warm ivory type and restrained sage accents.
- Branded top bar and live Focused/Balanced mode pill.
- Daily allowance surface with a native progress indicator.
- Intent-based action cards instead of an undifferentiated button list.
- Compact three-column boundaries summary.
- Rounded bottom navigation with an explicit selected state.
- Native ripple and press feedback, plus short panel/WebView transitions.
- Refined hierarchy, typography, line spacing, borders, elevation and touch targets.
- Three-step onboarding pagination and a clearer deliberate-end state.

The implementation uses Android platform Views and drawables only. It adds no runtime library, analytics SDK, network endpoint or permission.

## Interaction principles

Motion communicates a state change; it is not decorative. Android's system animation scale governs the native animations. The interface keeps labelled navigation and buttons rather than relying on ambiguous icons. Text is allowed to grow and surfaces use content-driven height wherever possible.

## Phone verification

Test on GrapheneOS with default settings, large text and TalkBack:

1. Confirm the first-run philosophy pages scroll fully and both final mode choices work.
2. Confirm no text is clipped in the header, daily surface, intent cards or boundary metrics.
3. Confirm Home, Inbox, Browse and Settings selected states match the current destination.
4. Confirm the progress surface updates while browsing and does not reset limits.
5. Confirm Focused mode omits the finite-feed action and Balanced mode displays it.
6. Confirm cards, primary actions and secondary actions have visible press feedback.
7. Confirm panel transitions are brief and do not flash the Instagram page.
8. Disable system animations and confirm the interface remains usable.
9. Use TalkBack to verify heading order, control labels and focus navigation.
10. Rotate or resize where supported and verify no content becomes unreachable.

## Honest boundary

This is a native interface redesign, not evidence that Instagram filtering has become perfect. Instagram still processes website activity and may change its markup or embedded-browser behaviour.
