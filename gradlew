#!/usr/bin/env sh
# Lightweight Gradle bootstrap for environments where the wrapper JAR is unavailable.
# Downloads the pinned Gradle distribution once, then delegates all arguments to it.
set -eu
GRADLE_VERSION="8.13"
BASE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/music-player-promax-bootstrap"
INSTALL_DIR="$BASE_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$INSTALL_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    mkdir -p "$BASE_DIR"
    ARCHIVE="$BASE_DIR/gradle-$GRADLE_VERSION-bin.zip"
    if [ ! -f "$ARCHIVE" ]; then
        if command -v curl >/dev/null 2>&1; then
            curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
        elif command -v wget >/dev/null 2>&1; then
            wget "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O "$ARCHIVE"
        else
            echo "Gradle bootstrap requires curl or wget." >&2
            exit 1
        fi
    fi
    if command -v unzip >/dev/null 2>&1; then
        unzip -q -o "$ARCHIVE" -d "$BASE_DIR"
    else
        echo "Gradle bootstrap requires unzip." >&2
        exit 1
    fi
fi

exec "$GRADLE_BIN" "$@"
