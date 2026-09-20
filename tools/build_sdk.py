"""Dependency-free SDK build, used for the supplied pilot. JDK17 + SDK35 required.
Usage: ATARAXIA_SIGNING_PASSWORD=... python tools/build_sdk.py --sdk SDK --jdk JDK --keystore FILE --output app.apk
Create a personal signing key beforehand using keytool. Never commit the key/password.
"""
import argparse, os, pathlib, subprocess, zipfile, shutil
p=argparse.ArgumentParser()
for name in ['sdk','jdk','keystore','output']:p.add_argument('--'+name,required=True)
a=p.parse_args(); root=pathlib.Path(__file__).resolve().parents[1]
sdk=pathlib.Path(a.sdk).resolve();jdk=pathlib.Path(a.jdk).resolve();bt=sdk/'build-tools/35.0.0'
out=pathlib.Path(a.output).resolve();key=pathlib.Path(a.keystore).resolve()
build=root/'build/sdk';build.mkdir(parents=True,exist_ok=True)
for name in ['classes','dex']:
 dest=build/name
 if dest.exists():shutil.rmtree(dest)
 dest.mkdir()
env=dict(os.environ,JAVA_HOME=str(jdk),PATH=str(jdk/'bin')+os.pathsep+os.environ['PATH'])
if not env.get('ATARAXIA_SIGNING_PASSWORD'):raise SystemExit('Set ATARAXIA_SIGNING_PASSWORD in your environment')
def run(*cmd):subprocess.run([str(x) for x in cmd],check=True,env=env)
android=sdk/'platforms/android-35/android.jar'
manifest=(root/'app/src/main/AndroidManifest.xml').read_text().replace('<manifest xmlns:android=', '<manifest package="app.ataraxia.social" xmlns:android=')
(build/'AndroidManifest.xml').write_text(manifest)
run(jdk/'bin/javac','-encoding','UTF-8','-source','17','-target','17','-classpath',android,'-d',build/'classes',*sorted((root/'app/src/main/java').rglob('*.java')))
run(jdk/'bin/jar','--create','--file',build/'classes.jar','-C',build/'classes','.')
run(bt/'d8','--release','--min-api','30','--lib',android,'--output',build/'dex',build/'classes.jar')
run(bt/'aapt2','compile','--dir',root/'app/src/main/res','-o',build/'resources.zip')
run(bt/'aapt2','link','-o',build/'unsigned.apk','-I',android,'--manifest',build/'AndroidManifest.xml','--min-sdk-version','30','--target-sdk-version','35','--version-code','6','--version-name','0.1.5-preview','-A',root/'app/src/main/assets',build/'resources.zip')
with zipfile.ZipFile(build/'unsigned.apk','a') as z:
 for dex in (build/'dex').glob('*.dex'):z.write(dex,dex.name)
run(bt/'zipalign','-f','-p','4',build/'unsigned.apk',build/'aligned.apk')
out.parent.mkdir(parents=True,exist_ok=True)
run(bt/'apksigner','sign','--ks',key,'--ks-key-alias','ataraxia-pilot','--ks-pass','env:ATARAXIA_SIGNING_PASSWORD','--key-pass','env:ATARAXIA_SIGNING_PASSWORD','--out',out,build/'aligned.apk')
run(bt/'apksigner','verify','--verbose',out)
run(bt/'aapt2','dump','permissions',out)
print('APK:',out)
