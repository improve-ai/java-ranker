This module is only used to build the xxhash shared library.

## Build the library
```
cd xxhash/src/main/jni
ndk-build
```

## Distribute
Copy the so files to the libs folder of module 'improveai-android'

### For ubuntu 22.XX development
1. (optional) set JAVA_HOME
2. cd xxhash/src/main/jni
3. gcc -I"${JAVA_HOME}/include" -I"${JAVA_HOME}/include/linux" --shared -o libxxhash.so -fPIC xxhash.c
4. (optional) copy craeted `libxxhash.so` to desired libs folder
5. update java.library.path in with the path to folder which contains `libxxhash.so`

