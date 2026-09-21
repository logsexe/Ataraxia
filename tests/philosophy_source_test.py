"""Regression checks for philosophy-led surfaces and free/privacy promise."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
activity=(root/"app/src/main/java/app/ataraxia/social/MainActivity.java").read_text()
chrome=(root/"app/src/main/java/app/ataraxia/social/AtaraxiaChrome.kt").read_text()
surfaces=(root/"app/src/main/java/app/ataraxia/social/AtaraxiaSurfaces.kt").read_text()
charter=(root/"docs/PHILOSOPHY.md").read_text()
for token in ['prefs.getBoolean("philosophyIntro",false)',"showPhilosophy(0,true)","AtaraxiaPhilosophyView",'putBoolean("philosophyIntro",true)']:
 assert token in activity,token
for token in ["ABOUT 4,000 WEEKS","roughly 4,174 weeks","ATTENTION","INTENTION","MODERATION","AGENCY","Begin in Focused mode","Begin in Balanced mode","Ataraxia is free and has no developer ads or analytics."]:
 assert token in surfaces,token
for token in ["A FINITE LIFE","You have about\\n4,000 weeks.","ATTENTION","INTENTION","ENOUGH","Enter with intention","Why Ataraxia?"]:
 assert token in chrome,token
for token in ["Attention is not merely a metric.","infinite feeds, autoplay chains or swipe-to-next loops","streaks, shame, punishment or moral scoring","developer advertising, sponsored ranking or payment-gated wellbeing controls","The app remains free."]:
 assert token in charter,token
print("PASS: philosophy remains present across landing, onboarding and ending")
