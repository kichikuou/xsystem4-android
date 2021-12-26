#!/bin/sh

set -e # make any subsequent failing command exit the script

if [ -z $ANDROID_NDK_HOME ]; then
    echo 'You need to set the ANDROID_NDK_HOME environment variable to point to your Android NDK.'
    exit 1
fi

ABI_NAMES=${ABI_NAMES:-armeabi-v7a arm64-v8a x86 x86_64}
ANDROID_API_LEVEL=${ANDROID_API_LEVEL:-21}

for abi in ${ABI_NAMES}; do
    cmake -B build/${abi} \
	  -S . \
	  -GNinja \
	  -DCMAKE_BUILD_TYPE=Release \
	  -DCMAKE_ANDROID_ARCH_ABI=${abi} \
	  -DANDROID_PLATFORM=${ANDROID_API_LEVEL} \
	  -DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake
    ninja -C build/${abi} install
done
