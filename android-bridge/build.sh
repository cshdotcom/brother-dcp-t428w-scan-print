#!/bin/bash
# PaperMarker Android 桥接应用构建脚本（无 Gradle）
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
# 工具链路径：优先环境变量（CI / 其他机器），否则回退本机路径
JAVA_HOME=${JAVA_HOME:-/home/z/android-tools/jdk-21.0.13+9}
export JAVA_HOME
export PATH=$JAVA_HOME/bin:$PATH
TOOLS=${ANDROID_TOOLS:-/home/z/android-tools}
BT=${ANDROID_BUILD_TOOLS:-$TOOLS/android-14}
AJAR=${ANDROID_JAR:-$TOOLS/android-34/android.jar}
KS="$DIR/papermarker.keystore"
OUT=$DIR/build
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/gen"

echo "[1/6] 生成最小资源"
mkdir -p "$OUT/res/values"
cat > "$OUT/res/values/strings.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">PaperMarker 桥接</string>
</resources>
EOF
cat > "$OUT/res/values/styles.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="@android:style/Theme.DeviceDefault.Light.NoActionBar"></style>
</resources>
EOF

echo "[2/6] aapt2 compile 资源 → link 生成 R.java + base.apk"
"$BT/aapt2" compile --dir "$OUT/res" -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$AJAR" \
  --manifest "$DIR/AndroidManifest.xml" \
  --java "$OUT/gen" \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 21 --version-name "2.0.1" \
  --auto-add-overlay \
  "$OUT/res.zip"

echo "[3/6] javac 编译"
find "$DIR/src" "$OUT/gen" -name "*.java" > "$OUT/sources.txt"
# 注意：不用 -bootclasspath 指向 android.jar（其中的 LambdaMetafactory 是残缺 stub，
# 会导致 lambda 报 metafactory 找不到）。改为 android.jar 仅作 -classpath，
# java.* 平台类由 JDK 提供，android.* 类从 android.jar 解析。
javac -source 8 -target 8 -nowarn -encoding UTF-8 \
  -classpath "$AJAR" \
  -d "$OUT/classes" @"$OUT/sources.txt" 2> "$OUT/javac_warn.txt" || { cat "$OUT/javac_warn.txt"; exit 1; }

echo "[4/6] d8 打包 dex"
cd "$OUT/classes" && "$BT/d8" --release --lib "$AJAR" --min-api 26 --output "$OUT" \
  $(find . -name '*.class')
cd "$DIR"

echo "[5/6] 合并 APK"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
cd "$OUT" && zip -j unsigned.apk classes.dex && cd "$DIR"

echo "[6/6] zipalign + 签名"
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$DIR/PaperMarker-Bridge.apk"
if [ ! -f "$KS" ]; then
  echo "  未找到 $KS，自动生成 debug 签名密钥…"
  keytool -genkey -v -keystore "$KS" -alias papermarker \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass papermarker2024 -keypass papermarker2024 \
    -dname "CN=PaperMarker, OU=Dev, O=PaperMarker, L=City, ST=State, C=CN"
fi
"$BT/apksigner" sign --ks "$KS" \
  --ks-pass pass:papermarker2024 --ks-key-alias papermarker \
  --key-pass pass:papermarker2024 \
  "$DIR/PaperMarker-Bridge.apk"

echo ""
echo "✓ 构建完成：$DIR/PaperMarker-Bridge.apk"
ls -lh "$DIR/PaperMarker-Bridge.apk"
