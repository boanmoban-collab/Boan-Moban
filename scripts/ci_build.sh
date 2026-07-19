#!/bin/bash
# Local CI Build Helper Script

echo "=== Preparing Build Environment for Maria Bot (LT_9904) ==="
gradle wrapper --gradle-version 8.7 --distribution-type all
chmod +x ./gradlew

echo "=== Running Unit Tests ==="
./gradlew test --no-daemon

echo "=== Assembling Release APK ==="
./gradlew assembleRelease --no-daemon
