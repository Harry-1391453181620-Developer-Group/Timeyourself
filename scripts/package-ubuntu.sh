#!/usr/bin/env bash
#
# Build TimeYourself for Ubuntu 22.04 / 24.04 / 26.04: a .deb plus a portable
# tar.gz.
#
# Build on Ubuntu 22.04 for a package that keeps the glibc 2.35 baseline and
# therefore runs on 22.04, 24.04 and 26.04. Building on 24.04 (glibc 2.39) or
# 26.04 (glibc 2.43) produces a package that will not run on 22.04; the script
# warns.
#
# Requirements:
#   - Ubuntu 22.04, 24.04 or 26.04
#   - JDK 26 (JAVA_HOME or java on PATH)
#   - Maven 3.6+
#   - dpkg, dpkg-deb, fakeroot (only for the .deb; SKIP_DEB=1 skips it)
#
# Output:
#   build/ubuntu/deb/timeyourself_<version>_<arch>.deb
#   build/ubuntu/TimeYourself-<version>-<arch>-ubuntu.tar.gz
#   build/ubuntu/SHA256SUMS
#
set -euo pipefail

APP_NAME="TimeYourself"
APP_VERSION="${APP_VERSION:-2.0.0}"
PACKAGE_NAME="timeyourself"
VENDOR="Harry-1391453181620 Developer Group"
# jpackage prepends the vendor: "Vendor <email>". This must be an email
# address only, not "Name <email>", or the control file becomes malformed.
MAINTAINER="${MAINTAINER:-timeyourself@users.noreply.github.com}"
DESCRIPTION="Time Yourself - offline task manager"
MODULES="java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.security.jgss,java.security.sasl,java.sql,java.sql.rowset,java.transaction.xa,java.xml,java.xml.crypto,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.zipfs"
# JavaFX native libraries are inside the Spring Boot jar, so jpackage's ldd
# scan cannot see them. Declare them explicitly. Ubuntu 24.04/26.04 renamed
# GTK, GLib and ALSA with a t64 suffix; the alternatives keep one .deb
# installable on 22.04, 24.04 and 26.04.
UBUNTU_DEPS="libgtk-3-0 | libgtk-3-0t64, libx11-6, libxext6, libxrender1, libxtst6, libxi6, libglib2.0-0 | libglib2.0-0t64, libfontconfig1, libfreetype6, libnss3, libasound2 | libasound2t64"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Linux" ]]; then
    echo "ERROR: this script must run on Ubuntu (jpackage cannot cross-build an Ubuntu package)." >&2
    exit 1
fi

if [[ ! -r /etc/os-release ]]; then
    echo "ERROR: /etc/os-release not found; only Ubuntu is supported." >&2
    exit 1
fi
# shellcheck disable=SC1091
. /etc/os-release

if [[ "${ID:-}" != "ubuntu" ]]; then
    echo "ERROR: only Ubuntu is supported (detected ID=${ID:-unknown})." >&2
    echo "       Only Ubuntu 22.04/24.04/26.04 is supported." >&2
    exit 1
fi

case "${VERSION_ID:-}" in
    22.04)
        ;;
    24.04)
        echo "WARN: building on Ubuntu 24.04. The .deb may require glibc 2.39" >&2
        echo "      and will not run on Ubuntu 22.04. Build on 22.04 (or use" >&2
        echo "      GitHub Actions) for a package that supports 22.04/24.04/26.04." >&2
        ;;
    26.04)
        echo "WARN: building on Ubuntu 26.04. The .deb may require glibc 2.43" >&2
        echo "      and will not run on Ubuntu 22.04/24.04. Build on 22.04 (or" >&2
        echo "      use GitHub Actions) for a package that supports all three." >&2
        ;;
    *)
        echo "WARN: tested Ubuntu versions are 22.04, 24.04 and 26.04;" >&2
        echo "      detected ${VERSION_ID:-unknown}. 25.04/25.10 are EOL." >&2
        ;;
esac

ARCH="$(uname -m)"
case "$ARCH" in
    x86_64|amd64)   ARCH="x86_64" ;;
    aarch64|arm64)  ARCH="aarch64" ;;
    *)
        echo "ERROR: unsupported architecture: $ARCH" >&2
        exit 1
        ;;
esac

if [[ -z "${JAVA_HOME:-}" ]]; then
    JAVA_BIN="$(command -v java || true)"
    if [[ -z "$JAVA_BIN" ]]; then
        echo "ERROR: java not found. Install JDK 26 (Temurin) or set JAVA_HOME." >&2
        exit 1
    fi
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")"
    export JAVA_HOME
fi

JAVA="$JAVA_HOME/bin/java"
JPACKAGE="$JAVA_HOME/bin/jpackage"
if [[ ! -x "$JAVA" || ! -x "$JPACKAGE" ]]; then
    echo "ERROR: JDK tools not found under $JAVA_HOME/bin (need a full JDK, not a JRE)." >&2
    exit 1
fi

JAVA_VERSION="$("$JAVA" -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
case "$JAVA_VERSION" in
    26*) ;;
    *)
        echo "ERROR: JDK 26 is required by pom.xml (release 26); found: ${JAVA_VERSION:-unknown}" >&2
        exit 1
        ;;
esac

if ! command -v mvn >/dev/null 2>&1; then
    echo "ERROR: Maven (mvn) not found. Install Maven 3.6+ first." >&2
    echo "       Ubuntu: sudo apt install maven" >&2
    exit 1
fi

SKIP_DEB="${SKIP_DEB:-0}"
if [[ "$SKIP_DEB" != "1" ]]; then
    for tool in dpkg dpkg-deb fakeroot; do
        if ! command -v "$tool" >/dev/null 2>&1; then
            echo "ERROR: $tool is required to build the .deb." >&2
            echo "       Ubuntu: sudo apt install -y fakeroot dpkg-dev" >&2
            echo "       To build only the portable tar.gz: SKIP_DEB=1 $0" >&2
            exit 1
        fi
    done
fi

echo "==> Ubuntu: ${PRETTY_NAME:-Ubuntu ${VERSION_ID:-unknown}}"
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

OUT="$ROOT/build/ubuntu"
rm -rf "$OUT"
mkdir -p "$OUT/input"
cp "$JAR" "$OUT/input/"

ICON="$ROOT/src/main/resources/static/images/appLogo-256.png"
if [[ ! -f "$ICON" ]]; then
    echo "WARN: appLogo-256.png not found; jpackage will use its default icon." >&2
    ICON=""
fi

COMMON_ARGS=(
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "$VENDOR"
    --description "$DESCRIPTION"
    --copyright "Copyright © 2026 $VENDOR"
    --input "$OUT/input"
    --main-jar "$(basename "$JAR")"
    --main-class org.springframework.boot.loader.launch.JarLauncher
    --add-modules "$MODULES"
    --java-options "-Dfile.encoding=UTF-8"
    --java-options "-Dstdout.encoding=UTF-8"
    --java-options "-Dstderr.encoding=UTF-8"
    --java-options "--enable-native-access=ALL-UNNAMED"
    # 打包版使用空闲端口，避免 8081 被占用；启动类会从 local.server.port 读回真实端口
    --java-options "-Dserver.port=0"
)
if [[ -n "$ICON" ]]; then
    COMMON_ARGS+=(--icon "$ICON")
fi

echo "==> Building portable app image..."
"$JPACKAGE" "${COMMON_ARGS[@]}" --type app-image --dest "$OUT/app"

APP="$OUT/app/$APP_NAME"
if [[ ! -x "$APP/bin/$APP_NAME" ]]; then
    echo "ERROR: jpackage did not create $APP/bin/$APP_NAME" >&2
    exit 1
fi

TARBALL="$OUT/${APP_NAME}-${APP_VERSION}-${ARCH}-ubuntu.tar.gz"
echo "==> Creating portable tarball..."
tar -C "$OUT/app" -czf "$TARBALL" "$APP_NAME"

DEB=""
if [[ "$SKIP_DEB" != "1" ]]; then
    echo "==> Building .deb..."
    mkdir -p "$OUT/deb"
    # --linux-shortcut is required: without it jpackage does not create the
    # .desktop file, so the application would not appear in the desktop menu.
    "$JPACKAGE" "${COMMON_ARGS[@]}" \
        --type deb \
        --dest "$OUT/deb" \
        --linux-package-name "$PACKAGE_NAME" \
        --linux-deb-maintainer "$MAINTAINER" \
        --linux-menu-group "Utility" \
        --linux-app-category "utils" \
        --linux-package-deps "$UBUNTU_DEPS" \
        --linux-shortcut

    DEB="$(find "$OUT/deb" -maxdepth 1 -name '*.deb' -print -quit)"
    if [[ -z "$DEB" || ! -f "$DEB" ]]; then
        echo "ERROR: jpackage did not produce a .deb under $OUT/deb" >&2
        exit 1
    fi

    # Fail the build early if the package is structurally broken.
    #
    # Do NOT use `dpkg-deb --contents ... | grep -q ...` here: grep -q exits
    # on the first match and closes the pipe while dpkg-deb's internal tar is
    # still writing. With `set -o pipefail`, that valid package is reported as
    # a failure ("tar: stdout: write error", exit status 2). Read the full
    # listing first, then match it without a pipe.
    dpkg-deb --info "$DEB" >/dev/null
    DEB_CONTENTS="$(dpkg-deb --contents "$DEB")"
    if [[ "$DEB_CONTENTS" != *"opt/$PACKAGE_NAME/bin/$APP_NAME"* ]]; then
        echo "ERROR: .deb does not contain opt/$PACKAGE_NAME/bin/$APP_NAME" >&2
        exit 1
    fi
    DEPENDS="$(dpkg-deb -f "$DEB" Depends)"
    if [[ "$DEPENDS" != *"libgtk-3-0"* ]]; then
        echo "ERROR: .deb does not declare the GTK runtime dependency." >&2
        exit 1
    fi
    if [[ "$DEPENDS" != *"libgtk-3-0t64"* ]]; then
        echo "ERROR: .deb is missing the Ubuntu 24.04/26.04 GTK dependency alias." >&2
        exit 1
    fi
    if [[ "$DEPENDS" != *"libasound2t64"* ]]; then
        echo "ERROR: .deb is missing the Ubuntu 24.04/26.04 ALSA dependency alias." >&2
        exit 1
    fi
fi

echo "==> Writing checksums..."
(
    cd "$OUT"
    {
        if [[ -n "$DEB" ]]; then
            sha256sum "deb/$(basename "$DEB")"
        fi
        sha256sum "$(basename "$TARBALL")"
    } > SHA256SUMS
    sha256sum -c SHA256SUMS
)

echo
echo "DONE"
echo "  App image: $APP"
echo "  Portable:  $TARBALL"
if [[ -n "$DEB" ]]; then
    echo "  DEB:       $DEB"
fi
echo "  Checksums: $OUT/SHA256SUMS"
echo "  Data:      ${XDG_DATA_HOME:-$HOME/.local/share}/timeyourself"
echo
if [[ -n "$DEB" ]]; then
    echo "Install: sudo apt install \"$DEB\""
else
    echo "Install: portable only (SKIP_DEB=1); run $APP/bin/$APP_NAME"
fi
