"""Narrow regression guard and review inventory, NOT an independent privacy audit."""
from pathlib import Path
import json,re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
ns='{http://schemas.android.com/apk/res/android}'
tools_ns='{http://schemas.android.com/tools}'
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
permissions=sorted(n.attrib[ns+'name'] for n in manifest.findall('uses-permission') if n.attrib.get(tools_ns+'node')!='remove')
assert permissions==['android.permission.INTERNET'],permissions
app=manifest.find('application')
assert app.attrib.get(ns+'allowBackup')=='false'
assert app.attrib.get(ns+'usesCleartextTraffic')=='false'
assert not app.findall('service'),'Review any new background service'
build=(root/'app/build.gradle').read_text()
dependencies=set(re.findall(r"implementation\s+(?:platform\()?['\"]([^'\"]+)",build))
allowed_dependencies={
 'androidx.compose:compose-bom:2024.12.01',
 'androidx.activity:activity-compose:1.9.3',
 'androidx.compose.ui:ui',
 'androidx.compose.ui:ui-tooling-preview',
 'androidx.compose.material3:material3',
}
assert dependencies==allowed_dependencies,('Review changed app dependencies',dependencies)
findings=[]
for p in sorted((root/'app/src/main').rglob('*')):
 if p.suffix not in {'.java','.kt','.js'}:continue
 text=p.read_text()
 # These detect common APIs only. Obfuscated or alternative implementations can evade this guard.
 for token in ['sendBeacon(', 'XMLHttpRequest', 'fetch(', 'WebSocket(', 'HttpURLConnection', 'OkHttpClient', 'FirebaseAnalytics', 'addJavascriptInterface(', 'Log.d(', 'Log.v(']:
  assert token not in text, f'Review {token} in {p}'
 findings.append({'file':str(p.relative_to(root)), 'https_literals':sorted(set(re.findall(r'https://[A-Za-z0-9./_-]+',text)))})
report={'scope':'Static regression checks only; not proof of no tracking or runtime network behaviour',
 'permissions':permissions,'application_dependencies':sorted(dependencies),
 'source_inventory':findings,'not_tested':['Instagram remote scripts','WebView/provider network traffic','runtime capture','reproducible APK comparison','independent human review']}
out=root/'build/privacy-review.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2)+'\n')
print('PASS: narrow privacy regression checks; inventory:',out)
