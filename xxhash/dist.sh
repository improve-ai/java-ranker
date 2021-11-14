#!/bin/bash

cd src/main

ndk-build

cp -R libs/* ../../../improveai-android/libs/
