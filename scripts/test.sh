#!/bin/sh
# Run unit tests + the conformance vector suite for persona-* modules.
set -e
cd "$(dirname "$0")/.."
. ./scripts/env.sh
./mvnw -T 1C verify "$@"
