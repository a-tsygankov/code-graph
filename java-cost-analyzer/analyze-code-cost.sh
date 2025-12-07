#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <directory-to-scan> [output-file]" >&2
    exit 1
fi

ROOT_DIR="$1"
OUT_FILE="${2:-java_cost_report_$(date +%Y%m%d_%H%M%S).toon}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

# Build fat jar if not present
if [ ! -f "target/java-cost-analyzer-1.0.0-jar-with-dependencies.jar" ]; then
    mvn -q -DskipTests package
fi

java -jar target/java-cost-analyzer-1.0.0-jar-with-dependencies.jar "$ROOT_DIR" "$OUT_FILE"
