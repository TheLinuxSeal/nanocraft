#!/bin/bash
set -e

./build.sh
for f in lib/*.jar; do unzip -o "$f" -d bin; done
rm -rf bin/META-INF/MANIFEST.MF
jar --create --file nanocraft.jar --main-class org.sutormin.nanocraft.Main -C bin .