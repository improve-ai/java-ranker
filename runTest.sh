#!/bin/bash
./gradlew clean
./gradlew :improveai:test --info
./gradlew :improveai-android:connectedAndroidTest --info
