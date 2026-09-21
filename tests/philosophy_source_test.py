"""Regression checks for the philosophy-led first-run journey.

These checks protect product copy and navigation wiring. The Android build still
provides the authoritative compile check, and physical UI behaviour must be
verified on-device.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
activity = (root / "app/src/main/java/app/ataraxia/social/MainActivity.java").read_text()
philosophy = (root / "docs/PHILOSOPHY.md").read_text()

required_activity = [
    'prefs.getBoolean("philosophyIntro",false)',
    "showPhilosophy(0,true)",
    "private void showPhilosophy(int page,boolean onboarding)",
    "About 4,000 weeks.",
    "roughly 4,174 weeks",
    "ATTENTION",
    "INTENTION",
    "MODERATION",
    "AGENCY",
    "Begin in Focused mode",
    "Begin in Balanced mode",
    "Why Ataraxia?",
    "Philosophy and purpose",
    "Ataraxia is free and has no developer ads or analytics.",
    'putBoolean("philosophyIntro",true)',
]
for text in required_activity:
    assert text in activity, f"Missing onboarding contract: {text}"

assert activity.count("showPhilosophy(0,true)") == 1, "First-run journey should have one automatic entry"
assert "showPhilosophy(0,false)" in activity, "The philosophy must remain voluntarily revisitable"
assert activity.index("Ataraxia is free") < activity.index("private void finishPhilosophy"), (
    "The free/privacy disclosure must appear before onboarding completes"
)

required_charter = [
    "Attention is not merely a metric.",
    "infinite feeds, autoplay chains or swipe-to-next loops",
    "streaks, shame, punishment or moral scoring",
    "developer advertising, sponsored ranking or payment-gated wellbeing controls",
    "The app remains free.",
]
for text in required_charter:
    assert text in philosophy, f"Missing philosophy charter constraint: {text}"

print(f"PASS: {len(required_activity)} onboarding and {len(required_charter)} philosophy assertions")
