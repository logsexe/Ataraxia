"""Static contract for the staged Material 3 Compose migration."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
activity = (root / "app/src/main/java/app/ataraxia/social/MainActivity.java").read_text()
chrome = (root / "app/src/main/java/app/ataraxia/social/AtaraxiaChrome.kt").read_text()
build = (root / "app/build.gradle").read_text()

legacy_contract = [
    'actionTile("Messages"',
    'actionTile("Following feed"',
    'metricRow("POSTS"',
    "private void stepDots(int page)",
    "setAccessibilityHeading(true)",
    "panel.animate().alpha(1f)",
    "web.animate().alpha(1f)",
    "private void selectNav(String label)",
    "AtaraxiaTopBarView",
    "AtaraxiaBottomBarView",
]
for token in legacy_contract:
    assert token in activity, f"Missing preserved shell contract: {token}"

compose_contract = [
    "darkColorScheme(",
    "MaterialTheme(",
    "NavigationBar(",
    "NavigationBarItem(",
    'Triple("Feed", "F", feedAction)',
    "animateColorAsState(",
    "semantics { heading() }",
    "RoundedCornerShape(24.dp)",
]
for token in compose_contract:
    assert token in chrome, f"Missing Compose UI contract: {token}"

for dependency in [
    "androidx.activity:activity-compose",
    "androidx.compose.ui:ui",
    "androidx.compose.material3:material3",
]:
    assert dependency in build, f"Missing reviewed Compose dependency: {dependency}"

for forbidden in ["LottieAnimationView", "FirebaseAnalytics", "com.google.android.gms"]:
    assert forbidden not in activity and forbidden not in chrome and forbidden not in build

assert '"Browse deliberately"' not in activity
assert activity.count("setDuration(") <= 6, "Keep motion restrained rather than decorative"
assert "INTERNET" not in activity and "INTERNET" not in chrome, "Permissions belong only in the reviewed manifest"

print(f"PASS: staged Compose Material 3 shell with preserved WebView controls")
