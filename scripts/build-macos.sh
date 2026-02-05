#!/usr/bin/env bash
set -euo pipefail

APP_NAME="InternalTool"
VERSION="1.0.0"
JAR_NAME="InternalTool-1.0-SNAPSHOT.jar"
DIST_DIR="dist"
INPUT_DIR="$DIST_DIR/input"

if ! command -v jpackage >/dev/null 2>&1; then
  echo "jpackage not found. Install JDK 21+ to get jpackage." >&2
  exit 1
fi

mvn -q -DskipTests package
mvn -q -DincludeScope=runtime -DoutputDirectory=target/dependency dependency:copy-dependencies

rm -rf "$DIST_DIR"
mkdir -p "$INPUT_DIR"

cp "target/$JAR_NAME" "$INPUT_DIR/"
cp target/dependency/*.jar "$INPUT_DIR/"

CLASSPATH=$(ls "$INPUT_DIR"/*.jar | xargs -n1 basename | paste -sd ":" -)

jpackage \
  --type dmg \
  --dest "$DIST_DIR" \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --input "$INPUT_DIR" \
  --main-jar "$JAR_NAME" \
  --main-class "org.example.Main" \
  --class-path "$CLASSPATH"

echo "Created $DIST_DIR/$APP_NAME-$VERSION.dmg"
