#!/usr/bin/env bash
# Собирает мод под все версии и складывает jar'ы в dist/.
# Нужна JDK 25 (для 26.x Loom не запускается на Java 21): JAVA_HOME=/путь/к/jdk-25 ./build-all.sh
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p dist
for v in 1.21.11 26.1 26.2 26.3; do
    echo "=== Minecraft $v"
    (cd "versions/$v" && ./gradlew dist --console=plain -q)
done
ls -1 dist
