#!/usr/bin/env bash
# 一键把网页版背卡打包成安卓 APK
# 用法：bash build_apk.sh [要打包的 html 路径]
# 依赖：D:/tools/androidbuild 下的 JDK17、android-15(build-tools)、android-35(platform)
set -euo pipefail

TOOLS=/d/tools/androidbuild
JAVA_HOME=$TOOLS/jdk/jdk-17.0.21.1+2
BT=$TOOLS/android-15
SDK_PLATFORM=$TOOLS/android-35
PROJ=$TOOLS/cet4app
SRC_HTML="${1:-/d/DeepSeekharness工作区/004-通用/四级搭配背卡.html}"
OUT_NAME="四级搭配背卡.apk"

cd "$PROJ"
cp "$SRC_HTML" assets/index.html

rm -rf build
mkdir -p build/gen build/classes build/dex

echo "[1/7] 编译资源"
"$BT/aapt2.exe" compile --dir res -o build/res.zip

echo "[2/7] 链接资源（含 assets 网页）"
"$BT/aapt2.exe" link -o build/base.apk \
  -I "$SDK_PLATFORM/android.jar" \
  --manifest AndroidManifest.xml \
  --java build/gen \
  -A assets \
  --min-sdk-version 23 --target-sdk-version 35 \
  build/res.zip

echo "[3/7] 编译 Java"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 8 -target 8 -nowarn \
  -classpath "$SDK_PLATFORM/android.jar" -d build/classes \
  $(find src build/gen -name "*.java")

echo "[4/7] 转成 dex"
"$JAVA_HOME/bin/java" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
  --release --min-api 23 --lib "$SDK_PLATFORM/android.jar" \
  --output build/dex $(find build/classes -name "*.class")

echo "[5/7] 装进 APK 并对齐"
python - <<'PY'
import shutil, zipfile
shutil.copy('build/base.apk','build/unsigned.apk')
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
    z.write('build/dex/classes.dex','classes.dex')
PY
"$BT/zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk

echo "[6/7] 签名"
if [ ! -f debug.jks ]; then
  "$JAVA_HOME/bin/keytool" -genkeypair -keystore debug.jks -alias cet4 \
    -storepass android -keypass android -dname "CN=CET4 Cards, O=Local" \
    -keyalg RSA -keysize 2048 -validity 10000 -noprompt
fi
"$JAVA_HOME/bin/java" -jar "$BT/lib/apksigner.jar" sign \
  --ks debug.jks --ks-pass pass:android --key-pass pass:android \
  --min-sdk-version 23 --v1-signing-enabled true --v2-signing-enabled true \
  --out "$OUT_NAME" build/aligned.apk

echo "[7/7] 校验"
# 老版 aapt.exe 读不了中文文件名，先复制成 ASCII 名再查
cp "$OUT_NAME" "$PROJ/_check.apk"
"$JAVA_HOME/bin/java" -jar "$BT/lib/apksigner.jar" verify --print-certs "$PROJ/_check.apk" | head -2
"$BT/aapt.exe" dump badging "$PROJ/_check.apk" | head -5
rm -f "$PROJ/_check.apk"

cp "$OUT_NAME" "/d/DeepSeekharness工作区/004-通用/$OUT_NAME"
ls -l "$OUT_NAME" "/d/DeepSeekharness工作区/004-通用/$OUT_NAME"
echo "OK: $(basename "$OUT_NAME")"