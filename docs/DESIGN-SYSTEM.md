# Ataraxia visual system

This is the implementation contract shared by the editable Figma source and Jetpack Compose. The visual direction borrows proven Material 3 structure, then replaces generic styling with Ataraxia's editorial, contemplative character. No third-party template code or runtime assets are imported.

## Principles

1. **Finite** — screens show an edge, a conclusion, and a way back to life.
2. **Intentional** — every primary action answers why the person opened the app.
3. **Quiet** — one dominant idea per screen; no streaks, badges, infinite carousels or decorative alerts.
4. **Honest** — privacy and filtering limitations remain legible.
5. **Free** — no premium state, upsell surface, advertising or telemetry.

## Tokens

| Role | Value | Use |
|---|---:|---|
| Night | `#07110D` | App background |
| Forest | `#0D1B15` | Raised navigation and metrics |
| Moss | `#15271F` | Interactive cards |
| Raised | `#1C3027` | Selected states |
| Sage | `#8DD9A6` | Primary action and progress |
| Sage soft | `#C2EBCF` | Supporting accent text |
| Ivory | `#F5F1E8` | Primary text |
| Mist | `#A6B7AD` | Supporting text |
| Line | `#2C4438` | Dividers and focus-safe outlines |
| Clay | `#E7B69E` | Rare reflective accent |

Typography uses the device serif for editorial display and the device sans-serif for UI. This keeps the app offline, avoids downloadable-font tracking/dependencies and preserves robust rendering. Display sizes are 48/50, 40/43 and 29/34 sp. Body sizes are 17/26 and 14/21 sp.

Spacing follows a 4 dp grid. Page gutters are 20–24 dp. Cards use 18–24 dp internal spacing. Corners are 14 dp for controls, 22 dp for cards and 30 dp for large reflective surfaces. Touch targets are at least 48 dp.

Motion is functional: 180 ms for direct feedback and 260 ms for page transitions. Use fade and short vertical translation only. Respect the system animation scale.

## Figma structure

Create these pages in the Material 3 Design Kit file:

- **00 Cover** — product promise and constraints
- **01 Foundations** — colour, type, spacing, radius, elevation and motion
- **02 Components** — eyebrow, editorial heading, primary/secondary button, intention card, metric, value row, mode chip, progress, navigation
- **03 Flow** — the seven screens below at 360×800 and 412×915
- **04 Accessibility** — large text, contrast, focus order and reduced-motion checks
- **05 Handoff** — component-to-Compose mapping

Use variables named `ataraxia/color/*`, `ataraxia/space/*`, `ataraxia/radius/*`, and `ataraxia/motion/*`. Components must use Auto Layout and semantic variant names. Do not detach imported Material components before token substitution.

## Seven-screen product flow

| Screen | Purpose | Primary action |
|---|---|---|
| Finite-life landing | Establish 4,000-week perspective | Enter with intention |
| Focused home | Make messages the only social destination | Open inbox |
| Balanced home | Offer a finite following feed | Open feed |
| Instagram shell | Keep limits/mode visible around web content | Return home |
| Boundaries | Adjust mode and local limits | Change one boundary |
| Deliberate ending | Turn a limit into reflection, not punishment | Return home / inbox |
| Philosophy | Explain finitude, ataraxia, moderation and agency | Choose mode |

## Template adaptation rules

- Use the official Material 3 kit for grids, states, accessibility and component anatomy.
- Use modern wellness/editorial references only for composition, rhythm and mood.
- Rebuild every surface with audited Compose primitives.
- Do not copy template source code, trackers, remote images, analytics, fonts or paid dependencies.
- Avoid fake social content and profile shortcuts: Feed means people already followed; profiles are not a destination.
- Every new dependency and Android permission requires privacy review.

## Compose mapping

| Design element | Source |
|---|---|
| Tokens and type | `AtaraxiaDesign.kt` |
| Landing, home, browser chrome, navigation | `AtaraxiaChrome.kt` |
| Settings, philosophy, ending | `AtaraxiaSurfaces.kt` |
| Navigation and WebView policy | `MainActivity.java` |

Pixel/GrapheneOS acceptance: first launch shows the finite-life landing; no legacy onboarding appears; Focused cannot open Feed; Balanced Feed is capped; Reels/Explore remain blocked; Settings is a full native surface; the ending survives reopening; only Internet permission is declared.
