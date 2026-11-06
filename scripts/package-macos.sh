#!/usr/bin/env bash
#
# Build a macOS .app and a drag-to-Applications .dmg for TimeYourself.
# Must run on macOS: jpackage cannot cross-build a Mac app from Windows.
#
set -euo pipefail

APP_NAME="TimeYourself"
APP_VERSION="${APP_VERSION:-2.0.0}"
BUNDLE_ID="com.timeyourself.app"
VENDOR="Harry-1391453181620 Developer Group"
DESCRIPTION="Time Yourself - offline task manager"
MODULES="java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.zipfs"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "ERROR: this script must run on macOS (jpackage cannot cross-build a Mac app)." >&2
    exit 1
fi

ARCH="$(uname -m)"
case "$ARCH" in
    arm64|x86_64) ;;
    *) echo "ERROR: unsupported Mac architecture: $ARCH" >&2; exit 1 ;;
esac

if [[ -z "${JAVA_HOME:-}" ]]; then
    if JAVA_HOME="$(/usr/libexec/java_home -v 26 2>/dev/null)"; then
        export JAVA_HOME
    else
        echo "ERROR: JDK 26 not found. Install it or set JAVA_HOME." >&2
        echo "       Example: export JAVA_HOME=\$(/usr/libexec/java_home -v 26)" >&2
        exit 1
    fi
fi

JAVA="$JAVA_HOME/bin/java"
JPACKAGE="$JAVA_HOME/bin/jpackage"
if [[ ! -x "$JPACKAGE" ]]; then
    echo "ERROR: jpackage not found under $JAVA_HOME/bin" >&2
    exit 1
fi

JAVA_VERSION="$("$JAVA" -version 2>&1 | awk -F '\"' '/version/ {print $2; exit}')"
case "$JAVA_VERSION" in
    26*) ;;
    *)
        echo "ERROR: JDK 26 is required by pom.xml (release 26); found: $JAVA_VERSION" >&2
        exit 1
        ;;
esac

if ! command -v mvn >/dev/null 2>&1; then
    echo "ERROR: Maven (mvn) not found. Install Maven 3.9+ first." >&2
    exit 1
fi

echo "==> JDK: $JAVA_VERSION ($ARCH)"
echo "==> Maven: $(mvn -version | head -n 1)"
echo "==> Building jar..."
mvn -DskipTests clean package

JAR="$ROOT/target/Time_Yourself-2.0.jar"
if [[ ! -f "$JAR" ]]; then
    JAR="$(find "$ROOT/target" -maxdepth 1 -name 'Time_Yourself-*.jar' -print -quit)"
fi
if [[ ! -f "$JAR" ]]; then
    echo "ERROR: Spring Boot jar was not produced under target/." >&2
    exit 1
fi

OUT="$ROOT/build/macos"
rm -rf "$OUT"
mkdir -p "$OUT/input"
cp "$JAR" "$OUT/input/"

# --- .icns from the 256px app logo -----------------------------------------
ICON_PNG="$ROOT/src/main/resources/static/images/appLogo-256.png"
ICONSET="$OUT/AppIcon.iconset"
ICON="$OUT/AppIcon.icns"
if [[ -f "$ICON_PNG" ]]; then
    echo "==> Building macOS icon..."
    mkdir -p "$ICONSET"
    make_icon() {
        sips -z "$1" "$1" "$ICON_PNG" --out "$ICONSET/$2" >/dev/null
    }
    make_icon 16   icon_16x16.png
    make_icon 32   icon_16x16@2x.png
    make_icon 32   icon_32x32.png
    make_icon 64   icon_32x32@2x.png
    make_icon 128  icon_128x128.png
    make_icon 256  icon_128x128@2x.png
    make_icon 256  icon_256x256.png
    make_icon 512  icon_256x256@2x.png
    make_icon 512  icon_512x512.png
    make_icon 1024 icon_512x512@2x.png
    iconutil -c icns "$ICONSET" -o "$ICON"
elif [[ -f "$ROOT/appLogo.icns" ]]; then
    cp "$ROOT/appLogo.icns" "$ICON"
else
    echo "WARN: appLogo-256.png not found; jpackage will use its default icon." >&2
fi

echo "==> Building .app with jpackage..."
JPACKAGE_ARGS=(
    --type app-image
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "$VENDOR"
    --description "$DESCRIPTION"
    --copyright "Copyright © 2026 $VENDOR"
    --input "$OUT/input"
    --main-jar "$(basename "$JAR")"
    --main-class org.springframework.boot.loader.launch.JarLauncher
    --dest "$OUT"
    --mac-package-identifier "$BUNDLE_ID"
    --mac-package-name "$APP_NAME"
    --mac-app-category public.app-category.productivity
    --add-modules "$MODULES"
    --java-options "-Dfile.encoding=UTF-8"
    --java-options "-Dstdout.encoding=UTF-8"
    --java-options "-Dstderr.encoding=UTF-8"
    --java-options "--enable-native-access=ALL-UNNAMED"
    --java-options "-Dserver.port=0"
    # Spring Boot unpacks the JavaFX native jars here. Use a stable absolute
    # path: macOS always has a writable /tmp, and no $HOME expansion is needed.
    --java-options "-Djava.io.tmpdir=/tmp"
)
if [[ -f "$ICON" ]]; then
    JPACKAGE_ARGS+=(--icon "$ICON")
fi
"$JPACKAGE" "${JPACKAGE_ARGS[@]}"

APP="$OUT/$APP_NAME.app"
if [[ ! -x "$APP/Contents/MacOS/$APP_NAME" ]]; then
    echo "ERROR: jpackage did not create $APP" >&2
    exit 1
fi

# The UI is served from http://127.0.0.1:<random port>. Allow local HTTP even
# if a future macOS/WebKit build applies App Transport Security to WebView.
PLIST="$APP/Contents/Info.plist"
if [[ -f "$PLIST" ]] && command -v /usr/libexec/PlistBuddy >/dev/null 2>&1; then
    /usr/libexec/PlistBuddy -c "Add :NSAppTransportSecurity dict" "$PLIST" 2>/dev/null || true
    /usr/libexec/PlistBuddy -c "Add :NSAppTransportSecurity:NSAllowsLocalNetworking bool true" "$PLIST" 2>/dev/null || true
fi

# macOS signs the launcher and the bundled JDK runtime separately. The main
# launcher must carry com.apple.security.cs.disable-library-validation, or it
# cannot load the JDK's libjli.dylib ("different Team IDs"). This is the
# jpackage issue JDK-8358723. Do NOT re-sign the runtime dylibs: leave the
# JDK vendor's signatures intact; only sign the launcher + app bundle.
echo "==> Signing (ad-hoc by default)..."
xattr -cr "$APP" 2>/dev/null || true
SIGN_ID="${CODESIGN_IDENTITY:--}"
ENTITLEMENTS="$ROOT/scripts/TimeYourself.entitlements"
SIGN_ARGS=(--force --sign "$SIGN_ID")
if [[ -f "$ENTITLEMENTS" ]]; then
    SIGN_ARGS+=(--entitlements "$ENTITLEMENTS")
else
    echo "WARN: $ENTITLEMENTS not found; libjli may fail with different Team IDs." >&2
fi

# Remove the old signature first so no stale hardened-runtime flags remain.
codesign --remove-signature "$APP/Contents/MacOS/$APP_NAME" 2>/dev/null || true
if codesign "${SIGN_ARGS[@]}" "$APP/Contents/MacOS/$APP_NAME"; then
    codesign "${SIGN_ARGS[@]}" "$APP" || true
    codesign --verify --strict --verbose=2 "$APP" || true
else
    echo "WARN: codesign failed; the DMG will still be created, but Gatekeeper may need a right-click Open." >&2
fi

echo "==> Creating DMG..."
STAGE="$OUT/dmg"
rm -rf "$STAGE"
mkdir -p "$STAGE"
ditto "$APP" "$STAGE/$APP_NAME.app"
ln -s /Applications "$STAGE/Applications"

DMG="$OUT/${APP_NAME}-${APP_VERSION}-${ARCH}.dmg"
rm -f "$DMG"
hdiutil create \
    -volname "$APP_NAME" \
    -srcfolder "$STAGE" \
    -ov \
    -format UDZO \
    "$DMG" >/dev/null

echo
echo "DONE"
echo "  App: $APP"
echo "  DMG: $DMG"
echo "  Data (first launch): ~/Library/Application Support/TimeYourself"
echo
echo "Install: open the DMG and drag TimeYourself into Applications."
