"""Static contract for the dependency-free modern Android visual system."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
activity = (root / "app/src/main/java/app/ataraxia/social/MainActivity.java").read_text()
build = (root / "app/build.gradle").read_text()

required = [
    "SOCIAL MEDIA, IN MEASURE",
    "private static final int BG = 0xff0b1411",
    "ACCENT_STRONG = 0xff7fd29b",
    "modePill=pill(",
    "updateModePill()",
    "ProgressBar progress=",
    'section("CHOOSE AN INTENTION")',
    'actionTile("Messages"',
    'addNav(nav,"Feed"',
    'actionTile("Following feed"',
    'metricRow("POSTS"',
    "private void stepDots(int page)",
    "RippleDrawable",
    "setAccessibilityHeading(true)",
    "panel.animate().alpha(1f)",
    "web.animate().alpha(1f)",
    "private void selectNav(String label)",
]
for token in required:
    assert token in activity, f"Missing modern UI contract: {token}"

for forbidden in ["com.google.android.material", "androidx.compose", "LottieAnimationView"]:
    assert forbidden not in activity and forbidden not in build, f"Unexpected UI dependency: {forbidden}"

assert '"Browse deliberately"' not in activity, "Feed navigation must use direct language"
assert "implementation " not in build and "implementation(" not in build, "UI redesign must not add runtime dependencies"
assert activity.count("setDuration(") <= 6, "Keep motion restrained rather than decorative"
assert "INTERNET" not in activity, "Permissions belong only in the reviewed manifest"

print(f"PASS: {len(required)} modern UI assertions; native dependency-free visual system")
