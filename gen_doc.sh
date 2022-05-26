#!/bin/bash
./gradlew :improveai:javadoc
cp -R improveai/build/docs/javadoc/* docs
