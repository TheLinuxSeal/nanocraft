#!/bin/bash
set -e

rm -rf bin
mkdir bin
javac -cp "lib/*" -d bin $(find src -name "*.java")
cp -r res/. bin