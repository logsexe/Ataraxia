"""Static contract for the mindful Material 3 interface."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
activity = (root / "app/src/main/java/app/ataraxia/social/MainActivity.java").read_text()
chrome = (root / "app/src/main/java/app/ataraxia/social/AtaraxiaChrome.kt").read_text()
build = (root / "app/build.gradle").read_text()

activity_contract = [
    "private void showLanding()",
    "AtaraxiaLandingView",
    "AtaraxiaHomeView",
    "showLanding();",
    'landing.setActions(()->{if(prefs.getBoolean("philosophyIntro",false))showHome();else showPhilosophy(0,true);}',
    "Focused — inbox only",
    "Legacy saved profiles are ignored",
    "private void selectNav(String label)",
]
for token in activity_contract:
    assert token in activity, f"Missing mindful shell contract: {token}"

compose_contract = [
    "darkColorScheme(",
    "MaterialTheme(",
    "NavigationBar(",
    "NavigationBarItem(",
    "You have about\\n4,000 weeks.",
    "Enter with intention",
    "BEGIN WITH A PURPOSE",
    "What are you\\nhere to do?",
    "Take one breath.",
    "Following feed",
    "“Enough” is not a failure to continue.",
    "LinearProgressIndicator(",
    "AnimatedVisibility(visible = !focused)",
    "Icons.Filled.Home",
    "RoundedCornerShape(24.dp)",
]
for token in compose_contract:
    assert token in chrome, f"Missing Compose design contract: {token}"

for dependency in [
    "androidx.activity:activity-compose",
    "androidx.compose.ui:ui",
    "androidx.compose.material3:material3",
]:
    assert dependency in build, f"Missing reviewed Compose dependency: {dependency}"

for removed in [
    "Visit a profile",
    "Someone specific",
    "Saved profiles",
    "private void visitProfile()",
    "private void showSavedProfiles()",
]:
    assert removed not in activity, f"Removed profile feature returned: {removed}"

for forbidden in ["LottieAnimationView", "FirebaseAnalytics", "com.google.android.gms"]:
    assert forbidden not in activity and forbidden not in chrome and forbidden not in build

assert '"Browse deliberately"' not in activity
assert activity.count("setDuration(") <= 6, "Keep motion restrained rather than decorative"
assert "INTERNET" not in activity and "INTERNET" not in chrome, "Permissions belong only in the reviewed manifest"

print("PASS: mindful landing, minimal Compose home, modern navigation and no profile shortcuts")
