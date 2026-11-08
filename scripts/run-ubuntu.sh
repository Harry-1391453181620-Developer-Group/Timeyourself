#!/usr/bin/env bash
#
# Run TimeYourself from source on Ubuntu 22.04 / 24.04 / 26.04.
# The app opens a native JavaFX window; the backend listens on 127.0.0.1:8081.
#
# Only Ubuntu is supported. Other distributions are rejected here on purpose
# so that unsupported environments fail loudly instead of silently.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Linux" ]]; then
    echo "ERROR: this helper is for Ubuntu. On Windows use: mvn spring-boot:run" >&2
    echo "       On macOS use: ./scripts/run-macos.sh" >&2
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
    22.04|24.04|26.04) ;;
    *)
        echo "WARN: tested on Ubuntu 22.04/24.04/26.04; detected Ubuntu ${VERSION_ID:-unknown}." >&2
        echo "      25.04/25.10 are end-of-life interim releases; upgrade to an LTS." >&2
        echo "      Continue at your own risk." >&2
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
if [[ ! -x "$JAVA" ]]; then
    echo "ERROR: $JAVA is not executable. Check JAVA_HOME=$JAVA_HOME" >&2
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

if [[ -z "${DISPLAY:-}" && -z "${WAYLAND_DISPLAY:-}" ]]; then
    echo "WARN: neither DISPLAY nor WAYLAND_DISPLAY is set; the JavaFX window cannot open." >&2
    echo "      Run this from a desktop session, not a bare SSH shell." >&2
fi

DATA_DIR="${TIMEYOURSELF_DATA_DIR:-${XDG_DATA_HOME:-$HOME/.local/share}/timeyourself}"
echo "==> Ubuntu: ${PRETTY_NAME:-Ubuntu ${VERSION_ID:-unknown}}"
echo "==> JDK: $JAVA_VERSION"
echo "==> Data: $DATA_DIR"

exec mvn spring-boot:run
