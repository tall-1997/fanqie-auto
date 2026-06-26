#!/bin/bash
set -e

echo "=== 构建 FanqieAuto APK ==="

PROJECT_DIR="/workspace/fanqie-auto"
BUILD_DIR="$PROJECT_DIR/build"
OUTPUT_DIR="$PROJECT_DIR/output"

# 清理
rm -rf "$BUILD_DIR" "$OUTPUT_DIR"
mkdir -p "$BUILD_DIR"/{classes,dex,res/values,res/drawable,assets,META-INF} "$OUTPUT_DIR"

# 1. 编译 Xposed stubs
echo "[1/7] 编译 Xposed stubs..."
mkdir -p /tmp/xposed_stubs/de/robv/android/xposed

cat > /tmp/xposed_stubs/de/robv/android/xposed/IXposedHookLoadPackage.java << 'JAVA'
package de.robv.android.xposed;
public interface IXposedHookLoadPackage {
    void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
}
JAVA

cat > /tmp/xposed_stubs/de/robv/android/xposed/XC_LoadPackage.java << 'JAVA'
package de.robv.android.xposed;
public class XC_LoadPackage {
    public static class LoadPackageParam {
        public String packageName;
        public ClassLoader classLoader;
    }
}
JAVA

cat > /tmp/xposed_stubs/de/robv/android/xposed/XC_MethodHook.java << 'JAVA'
package de.robv.android.xposed;
public abstract class XC_MethodHook {
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
    public static abstract class MethodHookParam {
        public Object thisObject;
        public Object[] args;
        public Object getResult() { return null; }
        public void setResult(Object result) {}
    }
}
JAVA

cat > /tmp/xposed_stubs/de/robv/android/xposed/XposedBridge.java << 'JAVA'
package de.robv.android.xposed;
public class XposedBridge {
    public static void log(String msg) {}
}
JAVA

cat > /tmp/xposed_stubs/de/robv/android/xposed/XposedHelpers.java << 'JAVA'
package de.robv.android.xposed;
public class XposedHelpers {
    public static Class<?> findClass(String name, ClassLoader cl) throws ClassNotFoundException {
        return Class.forName(name, false, cl);
    }
    public static void findAndHookMethod(Class<?> clazz, String method, Object... args) {}
    public static void findAndHookMethod(String className, ClassLoader cl, String method, Object... args) {}
    public static Object getObjectField(Object obj, String field) { return null; }
    public static void setObjectField(Object obj, String field, Object value) {}
    public static Object callMethod(Object obj, String method, Object... args) { return null; }
}
JAVA

javac -d "$BUILD_DIR/classes" /tmp/xposed_stubs/de/robv/android/xposed/*.java

# 2. 编译模块代码
echo "[2/7] 编译模块代码..."
javac -cp "$BUILD_DIR/classes" -d "$BUILD_DIR/classes" \
    "$PROJECT_DIR/app/src/main/java/com/fanqie/auto/MainHook.java"

# 3. 创建 DEX
echo "[3/7] 创建 DEX..."
cd "$BUILD_DIR/classes"
jar cf "$BUILD_DIR/fanqie-auto.jar" com/
cd "$BUILD_DIR"

# 使用 d8 创建 dex
if [ -f "/go/cache/d8" ]; then
    /go/cache/d8 --output "$BUILD_DIR/dex" "$BUILD_DIR/fanqie-auto.jar"
    mv "$BUILD_DIR/dex/classes.dex" "$BUILD_DIR/classes.dex"
else
    echo "错误: 未找到 d8 工具"
    exit 1
fi

# 4. 创建资源文件
echo "[4/7] 创建资源文件..."
cat > "$BUILD_DIR/res/values/strings.xml" << 'XML'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">FanqieAuto</string>
</resources>
XML

cat > "$BUILD_DIR/res/drawable/icon.xml" << 'XML'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#E94560" android:pathData="M54,54m-40,0a40,40 0,1 1,80 0a40,40 0,1 1,-80 0"/>
    <path android:fillColor="#FFFFFF" android:pathData="M40,40L40,68L68,54Z"/>
</vector>
XML

# 5. 创建 AndroidManifest.xml
echo "[5/7] 创建 AndroidManifest.xml..."
cat > "$BUILD_DIR/AndroidManifest.xml" << 'XML'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.fanqie.auto">
    <application
        android:label="FanqieAuto"
        android:icon="@drawable/icon">
        <meta-data android:name="xposedmodule" android:value="true"/>
        <meta-data android:name="xposeddescription" android:value="番茄小说自动签到模块 v0.0.1"/>
        <meta-data android:name="xposedminversion" android:value="82"/>
        <meta-data android:name="xposedscope" android:value="com.dragon.read"/>
    </application>
</manifest>
XML

echo "com.fanqie.auto.MainHook" > "$BUILD_DIR/assets/xposed_init"

# 6. 打包 APK
echo "[6/7] 打包 APK..."
cd "$BUILD_DIR"

aapt package -f -M AndroidManifest.xml \
    -S res \
    -I /usr/share/android/android.jar \
    -F "$OUTPUT_DIR/fanqie-auto-unsigned.apk" 2>/dev/null || true

cd "$BUILD_DIR"
zip -j "$OUTPUT_DIR/fanqie-auto-unsigned.apk" classes.dex 2>/dev/null || true
zip -j "$OUTPUT_DIR/fanqie-auto-unsigned.apk" assets/xposed_init 2>/dev/null || true

# 7. 签名 APK
echo "[7/7] 签名 APK..."
keytool -genkey -v -keystore "$BUILD_DIR/debug.keystore" \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android \
    -dname "CN=Debug,O=Android,C=US" 2>/dev/null

apksigner sign --ks "$BUILD_DIR/debug.keystore" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$OUTPUT_DIR/fanqie-auto.apk" \
    "$OUTPUT_DIR/fanqie-auto-unsigned.apk" 2>/dev/null || \
cp "$OUTPUT_DIR/fanqie-auto-unsigned.apk" "$OUTPUT_DIR/fanqie-auto.apk"

echo ""
echo "=== 构建完成 ==="
ls -la "$OUTPUT_DIR/"
