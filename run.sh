#!/bin/bash
set -e

./build.sh
java --enable-native-access=ALL-UNNAMED -cp "bin:lib/*" org.sutormin.nanocraft.Main