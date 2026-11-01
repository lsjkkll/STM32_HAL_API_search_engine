#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

BUILD="build"
DIST="dist"
JAVAC_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAR_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}jar"

rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$DIST"

find src/main/java -name '*.java' > "$BUILD/sources.txt"
"$JAVAC_BIN" --release 17 -encoding UTF-8 -d "$BUILD/classes" @"$BUILD/sources.txt"
cp -R src/main/resources/. "$BUILD/classes/"

"$JAR_BIN" --create --file "$DIST/HAL-Speed-Lookup-Desktop-1.0.0.jar" \
    --main-class com.stm32hal.api.desktop.Main -C "$BUILD/classes" .
echo "built $DIST/HAL-Speed-Lookup-Desktop-1.0.0.jar"
