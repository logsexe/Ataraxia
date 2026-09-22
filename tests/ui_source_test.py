"""Static contract for the Ataraxia editorial Material 3 system."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
activity=(root/"app/src/main/java/app/ataraxia/social/MainActivity.java").read_text()
chrome=(root/"app/src/main/java/app/ataraxia/social/AtaraxiaChrome.kt").read_text()
design=(root/"app/src/main/java/app/ataraxia/social/AtaraxiaDesign.kt").read_text()
surfaces=(root/"app/src/main/java/app/ataraxia/social/AtaraxiaSurfaces.kt").read_text()
build=(root/"app/build.gradle").read_text()
for token in ["AtaraxiaLandingView","AtaraxiaSettingsView","AtaraxiaPhilosophyView","AtaraxiaEndView","private void showLanding()","private void beginOnboarding()","private void selectNav(String label)"]:
 assert token in activity+chrome+surfaces,token
for token in ["darkColorScheme(","Typography(","FontFamily.Serif","FontFamily.SansSerif","QuickMotion = 180","GentleMotion = 260","RoundedCornerShape(30.dp)"]:
 assert token in design,token
for token in ["You have about\\n4,000 weeks.","Enter with intention","Following feed","ATARAXIA · ἀταραξία","A calm, untroubled mind—freedom from unnecessary disturbance.","fillMaxHeight()","Arrangement.Center"]:
 assert token in chrome,token
for token in ["Boundaries,\\nchosen by you.","Free forever · No developer ads · No analytics","Enough\\nfor now.","AnimatedContent("]:
 assert token in surfaces,token
for token in [
 'if (prefs.getBoolean("philosophyIntro",false)) navigate(BASE+"/direct/inbox/");',
 "else showLanding();","showChrome(false)","now-lastSnapshotPoll>=SNAPSHOT_POLL_MS",
 "setOverScrollMode(View.OVER_SCROLL_NEVER)","new LinearLayout.LayoutParams(-1,dp(88))",
 'composeBottomBar.setActions(()->navigate(BASE+"/direct/inbox/"),()->openFeed(),()->settings())',
]:
 assert token in activity,token
assert "SNAPSHOT_POLL_MS = 200L" in activity
assert "SNAPSHOT_TIMEOUT_MS = 2000L" in activity
assert "if (request!=snapshotRequest) return;" in activity
assert "if (seen.size()!=previousCount) {" in activity
assert "minLines = 2" in chrome
assert "TICK_MS = 1000L" in activity
assert "handler.post(snapshotter)" in activity
assert "handler.removeCallbacks(snapshotter)" in activity
assert "NavigationBarItem(" not in chrome
assert 'NavItem("Home"' not in chrome
assert chrome.count('NavItem("')==3
assert "verticalScroll(" not in surfaces, "Compose surfaces already live inside the activity ScrollView"
for removed in ["Visit a profile","Someone specific","Saved profiles","private void visitProfile()","private void showSavedProfiles()"]:
 assert removed not in activity+chrome+surfaces,removed
for forbidden in ["LottieAnimationView","FirebaseAnalytics","com.google.android.gms"]:
 assert forbidden not in activity+chrome+design+surfaces+build,forbidden
assert "INTERNET" not in activity+chrome+design+surfaces
print("PASS: first-run philosophy, centred three-item navigation and stable polling")
