cmake_minimum_required(VERSION 3.10)

# Rebuild external projects when download URL changes
if (CMAKE_VERSION VERSION_GREATER_EQUAL "3.24.0")
  cmake_policy(SET CMP0135 NEW)
endif()

project(xsystem4-android)

include(ExternalProject)

set(stagingDir ${CMAKE_CURRENT_BINARY_DIR}/stage)
set(androidProjectDir ${CMAKE_CURRENT_LIST_DIR}/project)

set(COMMON_CMAKE_ARGS
  -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
  -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
  -DCMAKE_FIND_ROOT_PATH=${stagingDir}
  -DCMAKE_ANDROID_ARCH_ABI=${CMAKE_ANDROID_ARCH_ABI}
  -DANDROID_PLATFORM=${ANDROID_PLATFORM}
  -DCMAKE_STAGING_PREFIX:PATH=${stagingDir}
  -DCMAKE_INSTALL_PREFIX:PATH=${stagingDir}
  -DANDROID_USE_LEGACY_TOOLCHAIN_FILE=OFF
  # https://github.com/android/ndk/issues/2032
  -DCMAKE_POLICY_DEFAULT_CMP0057=NEW
)

ExternalProject_Add(
  libogg
  URL https://github.com/xiph/ogg/releases/download/v1.3.5/libogg-1.3.5.tar.xz
  URL_HASH SHA1=5a368421a636f7faa4c2f662857cb507dffd7c99
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DCMAKE_POLICY_VERSION_MINIMUM=3.5
)

ExternalProject_Add(
  libvorbis
  URL https://github.com/xiph/vorbis/releases/download/v1.3.7/libvorbis-1.3.7.tar.xz
  URL_HASH SHA1=0a2dd71a999656b8091506839e8007a61a8fda1f
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DCMAKE_POLICY_VERSION_MINIMUM=3.5
  )

ExternalProject_Add_StepDependencies(libvorbis configure libogg)

ExternalProject_Add(
  libjpeg-turbo
  URL https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/3.1.0/libjpeg-turbo-3.1.0.tar.gz
  URL_HASH SHA1=ba3c0152f14a504018de19c9f62250d8f3351525
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
)

ExternalProject_Add(
  libwebp
  URL https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-1.3.2.tar.gz
  URL_HASH SHA1=f941661a869df7347d7ca9208acb4e6d41f614de
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DWEBP_BUILD_ANIM_UTILS=OFF
             -DWEBP_BUILD_CWEBP=OFF
             -DWEBP_BUILD_DWEBP=OFF
             -DWEBP_BUILD_GIF2WEBP=OFF
             -DWEBP_BUILD_IMG2WEBP=OFF
             -DWEBP_BUILD_VWEBP=OFF
             -DWEBP_BUILD_WEBPINFO=OFF
             -DWEBP_BUILD_WEBPMUX=OFF
             -DWEBP_BUILD_EXTRAS=OFF
             -DWEBP_BUILD_WEBP_JS=OFF
)

ExternalProject_Add(
  libpng
  URL https://download.sourceforge.net/libpng/libpng-1.6.47.tar.xz
  URL_HASH SHA256=b213cb381fbb1175327bd708a77aab708a05adde7b471bc267bd15ac99893631
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DPNG_SHARED=OFF
             -DPNG_ARM_NEON=on
)

set(TARGET_CC "${CMAKE_ANDROID_NDK_TOOLCHAIN_UNIFIED}/bin/clang -target ${CMAKE_C_COMPILER_TARGET}")

ExternalProject_Add(
  libffi
  URL https://github.com/libffi/libffi/releases/download/v3.4.6/libffi-3.4.6.tar.gz
  URL_HASH SHA1=19251dfee520dff42acefe36bfe76d7168071e01
  CONFIGURE_COMMAND <SOURCE_DIR>/configure
    --host ${CMAKE_C_COMPILER_TARGET}
    --prefix=${stagingDir}
    --disable-shared
    --disable-dependency-tracking
    --disable-builddir
    --disable-multi-os-directory
    --disable-structs
    --disable-raw-api
    --disable-docs
    AR=${CMAKE_AR}
    CC=${TARGET_CC}
    AS=${TARGET_CC}
    LD=${CMAKE_ANDROID_NDK_TOOLCHAIN_UNIFIED}/bin/ld
    RANLIB=${CMAKE_RANLIB}
    STRIP=${CMAKE_STRIP}
)

# This fork disables FLAC and Opus support.
ExternalProject_Add(
  libsndfile
  GIT_REPOSITORY https://github.com/kichikuou/libsndfile.git
  GIT_TAG be82f4be73c77f1e759bda9dfe1625204441465a
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DBUILD_SHARED_LIBS=OFF
             -DBUILD_PROGRAMS=OFF
             -DBUILD_EXAMPLES=OFF
             -DBUILD_TESTING=OFF
             -DENABLE_CPACK=OFF
)

ExternalProject_Add(
  libdeflate
  URL https://github.com/ebiggers/libdeflate/releases/download/v1.25/libdeflate-1.25.tar.gz
  URL_HASH SHA1=7e70557d094b54c3ee16f6c012976dc8d685b22a
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DCMAKE_POLICY_VERSION_MINIMUM=3.5
             -DLIBDEFLATE_BUILD_SHARED_LIB=OFF
             -DLIBDEFLATE_GZIP_SUPPORT=OFF
             -DLIBDEFLATE_BUILD_GZIP=OFF
)

ExternalProject_Add_StepDependencies(libsndfile configure libvorbis)

ExternalProject_Add(
  xsystem4
  SOURCE_DIR ${CMAKE_CURRENT_LIST_DIR}/xsystem4
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DCMAKE_MODULE_PATH=${CMAKE_CURRENT_LIST_DIR}/cmake
  BUILD_ALWAYS YES
)

ExternalProject_Add_StepDependencies(xsystem4 configure
  libjpeg-turbo libwebp libpng libffi libsndfile libdeflate)


install(
  FILES ${stagingDir}/lib/libxsystem4.so
        ${stagingDir}/lib/libSDL2.so
        ${stagingDir}/lib/libcglm.so
  DESTINATION ${androidProjectDir}/app/src/main/jniLibs/${CMAKE_ANDROID_ARCH_ABI})

install(
  DIRECTORY xsystem4/shaders xsystem4/fonts
  DESTINATION ${androidProjectDir}/app/src/main/assets)
