#!/bin/sh
# Build all persona-* modules (default reactor; legacy modules excluded).
set -e
cd "$(dirname "$0")/.."
. ./scripts/env.sh
./mvnw -T 1C clean install "$@"
