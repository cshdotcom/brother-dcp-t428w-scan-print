#!/bin/bash
set -e
export JAVA_HOME=/home/user/tools/jdk-17.0.13+11
export PATH="$JAVA_HOME/bin:$PATH"
SDK=/home/user/tools/sdk
BT=$SDK/build-tools/34.0.0
PLATFORM=$SDK/platforms/android-34/android.jar
SRC=/home/user/Doubao/chats/38453118839411714/测试APK源码
WORK=/tmp/apkbuild
OUTDIR=/home/user/Doubao/chats/38453118839411714

rm -rf "$WORK"
mkdir -p "$WORK/classes" "$WORK/dex" "$WORK/gen"

echo "== 1. aapt2 link =="
"$BT/aapt2" link \
  -I "$PLATFORM" \
  --manifest "$SRC/AndroidManifest.xml" \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  --version-code 6 \
  --version-name 0.6 \
  -o "$WORK/base.apk"

echo "== 2. javac =="
find "$SRC/src" -name "*.java" > "$WORK/sources.txt"
javac -source 8 -target 8 -bootclasspath "$PLATFORM" -encoding UTF-8 \
  -d "$WORK/classes" @"$WORK/sources.txt"

echo "== 3. d8 =="
find "$WORK/classes" -name "*.class" > "$WORK/classes.txt"
"$BT/d8" --release --min-api 21 --lib "$PLATFORM" \
  --output "$WORK/dex" @"$WORK/classes.txt"

echo "== 4. 打包 dex =="
cd "$WORK"
cp base.apk unsigned.apk
zip -q -j unsigned.apk dex/classes.dex

echo "== 5. zipalign =="
"$BT/zipalign" -f 4 unsigned.apk aligned.apk

echo "== 6. 签名 =="
KS="$HOME/.android/debug.keystore"
if [ ! -f "$KS" ]; then
  mkdir -p "$HOME/.android"
  keytool -genkeypair -v -keystore "$KS" -storepass android -alias androiddebugkey \
    -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$OUTDIR/BrotherTestLab-v0.6.apk" aligned.apk

echo "== 7. 校验 =="
"$BT/apksigner" verify --print-certs "$OUTDIR/BrotherTestLab-v0.6.apk" | head -4
"$BT/aapt2" dump badging "$OUTDIR/BrotherTestLab-v0.6.apk" | head -6
ls -lh "$OUTDIR/BrotherTestLab-v0.6.apk"
