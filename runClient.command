#!/bin/bash
# Double-click this file to launch Minecraft with the Void Invoker mod
cd "$(dirname "$0")"
./gradlew runClient -x createMinecraftArtifacts
