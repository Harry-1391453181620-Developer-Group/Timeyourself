#!/usr/bin/env bash
#
# Run TimeYourself from source on macOS.
# The app opens a native JavaFX window; the backend listens on 127.0.0.1:8081.
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "ERROR: this helper is for macOS. On Windows use: mvn spring-boot:run" >&2
    exit 1
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
    if JAVA_HOME="$(/usr/libexec/java_home -v 26 2>/dev/null)"; then
        export JAVA_HOME
    else
        echo "ERROR: JDK 26 not found. Install it or set JAVA_HOME." >&2
        exit 1
    fi
fi

exec mvn spring-boot:run
