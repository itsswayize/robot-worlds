#!/usr/bin/env bash
# Install the bundled eodsql.jar into the local Maven repository.
# Usage: ./scripts/install-eodsql.sh
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR_PATH="$ROOT_DIR/eodsql.jar"
if [ ! -f "$JAR_PATH" ]; then
  echo "Error: $JAR_PATH not found. Put eodsql.jar in the project root or adjust the script." >&2
  exit 2
fi
mvn install:install-file \
  -Dfile="$JAR_PATH" \
  -DgroupId=net.lemnik \
  -DartifactId=eodsql \
  -Dversion=2.2 \
  -Dpackaging=jar \
  -DgeneratePom=true
echo "Installed $JAR_PATH as net.lemnik:eodsql:2.2 into the local Maven repo."

