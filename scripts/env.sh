#!/bin/sh
# Development environment for the Persona modules (JDK 21 via Homebrew).
# Usage: source scripts/env.sh

if [ -d /opt/homebrew/opt/openjdk@21 ]; then
    JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
elif [ -x /usr/libexec/java_home ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null)"
fi

if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JDK 21 not found. Install with: brew install openjdk@21" >&2
    return 1 2>/dev/null || exit 1
fi

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
echo "JAVA_HOME=$JAVA_HOME"
