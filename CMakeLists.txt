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
)

ExternalProject_Add(
  libogg
  URL https://github.com/xiph/ogg/releases/download/v1.3.5/libogg-1.3.5.tar.xz
  URL_HASH SHA1=5a368421a636f7faa4c2f662857cb507dffd7c99
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
)

ExternalProject_Add(
  libvorbis
  URL https://github.com/xiph/vorbis/releases/download/v1.3.7/libvorbis-1.3.7.tar.xz
  URL_HASH SHA1=0a2dd71a999656b8091506839e8007a61a8fda1f
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
)

ExternalProject_Add_StepDependencies(libvorbis configure libogg)

ExternalProject_Add(
  flac
  URL https://github.com/xiph/flac/releases/download/1.4.3/flac-1.4.3.tar.xz
  URL_HASH SHA256=6c58e69cd22348f441b861092b825e591d0b822e106de6eb0ee4d05d27205b70
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DBUILD_CXXLIBS=OFF
             -DBUILD_PROGRAMS=OFF
             -DBUILD_EXAMPLES=OFF
             -DBUILD_TESTING=OFF
             -DBUILD_DOCS=OFF
             -DWITH_FORTIFY_SOURCE=OFF
             -DWITH_STACK_PROTECTOR=OFF
             -DINSTALL_MANPAGES=OFF
             -DENABLE_MULTITHREADING=OFF
             # TODO: Remove this when setting ANDROID_PLATFORM to >=24
             -DCMAKE_C_FLAGS="-Wno-error=implicit-function-declaration"
  BUILD_COMMAND cmake --build <BINARY_DIR> --target FLAC
  INSTALL_COMMAND
)

ExternalProject_Add_StepDependencies(flac configure libogg)

ExternalProject_Add(
  opus
  URL https://github.com/xiph/opus/releases/download/v1.4/opus-1.4.tar.gz
  URL_HASH SHA1=c53b3a74fc26fb2c2314afe958be30d169bd029a
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DOPUS_HARDENING=OFF
             -DOPUS_STACK_PROTECTOR=OFF
             -DOPUS_FORTIFY_SOURCE=OFF
)

ExternalProject_Add(
  libjpeg-turbo
  URL https://sourceforge.net/projects/libjpeg-turbo/files/2.1.2/libjpeg-turbo-2.1.2.tar.gz/download
  URL_HASH SHA1=65c51c543b1fbba6db9ff5bee474ccb0b52a929f
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
  URL https://download.sourceforge.net/libpng/libpng-1.6.37.tar.xz
  URL_HASH SHA1=3ab93fabbf4c27e1c4724371df408d9a1bd3f656
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DPNG_SHARED=OFF
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

ExternalProject_Add(
  libsndfile
  URL https://github.com/libsndfile/libsndfile/releases/download/1.2.2/libsndfile-1.2.2.tar.xz
  URL_HASH SHA1=8d9031e13f56abb79e89a122333ad11fa0512799
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
             -DBUILD_SHARED_LIBS=OFF
             -DBUILD_PROGRAMS=OFF
             -DBUILD_EXAMPLES=OFF
             -DBUILD_TESTING=OFF
             -DENABLE_CPACK=OFF
)

ExternalProject_Add_StepDependencies(libsndfile configure libvorbis flac opus)

ExternalProject_Add(
  xsystem4
  SOURCE_DIR ${CMAKE_CURRENT_LIST_DIR}/xsystem4
  CMAKE_ARGS ${COMMON_CMAKE_ARGS}
  BUILD_ALWAYS YES
)

ExternalProject_Add_StepDependencies(xsystem4 configure
  libjpeg-turbo libwebp libpng libffi libsndfile)


install(
  FILES ${stagingDir}/lib/libxsystem4.so
        ${stagingDir}/lib/libSDL2.so
        ${stagingDir}/lib/libcglm.so
  DESTINATION ${androidProjectDir}/app/src/main/jniLibs/${CMAKE_ANDROID_ARCH_ABI})

install(
  DIRECTORY xsystem4/shaders xsystem4/fonts
  DESTINATION ${androidProjectDir}/app/src/main/assets)
