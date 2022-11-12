#!/bin/bash
./gradlew :improveai:javadoc
cp -R improveai/build/docs/javadoc/* docs/javadoc

cp README.md docs/index.md
