#!/bin/bash
# PaperMarker Android 原生构建脚本（无 Gradle：aapt2 → javac → d8 → zipalign → apksigner）
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
# 工具链路径：优先环境变量（CI / 其他机器），否则回退本机路径
JAVA_HOME=${JAVA_HOME:-/home/z/android-tools/jdk-21.0.13+9}
export JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH
TOOLS=${ANDROID_TOOLS:-/home/z/android-tools}
BT=${ANDROID_BUILD_TOOLS:-$TOOLS/android-14}            # build-tools 34.0.0
AJAR=${ANDROID_JAR:-$TOOLS/android-34/android.jar}
KS="$DIR/papermarker.keystore"
OUT=$DIR/build
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/gen"

echo "[1/7] aapt2 compile 资源"
"$BT/aapt2" compile --dir "$DIR/res" -o "$OUT/res.zip"

echo "[2/7] aapt2 link 生成基础APK + R.java"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$AJAR" \
  --manifest "$DIR/AndroidManifest.xml" \
  --java "$OUT/gen" \
  --min-sdk-version 24 --target-sdk-version 34 \
  --version-code 2 --version-name "1.1" \
  --auto-add-overlay \
  "$OUT/res.zip"

echo "[3/7] javac 编译 Java 源码"
find "$DIR/src" "$OUT/gen" -name "*.java" > "$OUT/sources.txt"
javac -source 8 -target 8 -nowarn -encoding UTF-8 \
  -bootclasspath "$AJAR" -classpath "$AJAR" \
  -d "$OUT/classes" @"$OUT/sources.txt" 2> "$OUT/javac_warn.txt" || { cat "$OUT/javac_warn.txt"; exit 1; }
grep -v "warning" "$OUT/javac_warn.txt" || true

echo "[4/7] d8 转换为 dex"
(cd "$OUT/classes" && find . -name '*.class' -print0 | xargs -0 /home/z/android-tools/android-15/d8 --release --lib "$AJAR" --min-api 24 --output "$OUT")

echo "[5/7] 打包 classes.dex 进 APK"
(cd "$OUT" && zip -q -j base.apk classes.dex)

echo "[6/7] zipalign 对齐"
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"

echo "[7/7] apksigner 签名"
if [ ! -f "$KS" ]; then
  echo "  未找到 $KS，自动生成 debug 签名密钥…"
  keytool -genkey -v -keystore "$KS" -alias papermarker \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass papermarker2024 -keypass papermarker2024 \
    -dname "CN=PaperMarker, OU=Dev, O=PaperMarker, L=City, ST=State, C=CN"
fi
"$BT/apksigner" sign \
  --ks "$KS" \
  --ks-key-alias papermarker \
  --ks-pass pass:papermarker2024 \
  --key-pass pass:papermarker2024 \
  --out "$DIR/PaperMarker.apk" "$OUT/aligned.apk"

echo ""
echo "构建成功: $DIR/PaperMarker.apk"
ls -la "$DIR/PaperMarker.apk"
