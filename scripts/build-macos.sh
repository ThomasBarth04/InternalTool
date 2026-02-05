#!/usr/bin/env bash
set -euo pipefail

APP_NAME="InternalTool"
VERSION="1.0.0"
DIST_DIR="dist"
INPUT_DIR="$DIST_DIR/input"

if ! command -v jpackage >/dev/null 2>&1; then
  echo "jpackage not found. Install JDK 21+ to get jpackage." >&2
  exit 1
fi

mvn -q -DskipTests package

rm -rf "$DIST_DIR"
mkdir -p "$INPUT_DIR"

JAR_NAME=$(ls target/*.jar | grep -v "/original-" | head -n 1)
if [[ -z "$JAR_NAME" ]]; then
  echo "Shaded JAR not found. Make sure mvn package succeeds." >&2
  exit 1
fi
cp "$JAR_NAME" "$INPUT_DIR/"

jpackage \
  --type dmg \
  --dest "$DIST_DIR" \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --input "$INPUT_DIR" \
  --main-jar "$(basename "$JAR_NAME")"

echo "Created $DIST_DIR/$APP_NAME-$VERSION.dmg"
