#!/bin/sh

build_dir=$1
dest_dir=$2

mkdir -p ${dest_dir}
cp ${build_dir}/libogg-prefix/src/libogg/COPYING ${dest_dir}/libogg
cp ${build_dir}/libvorbis-prefix/src/libvorbis/COPYING ${dest_dir}/libvorbis
cp ${build_dir}/flac-prefix/src/flac/COPYING.Xiph ${dest_dir}/flac
cp ${build_dir}/opus-prefix/src/opus/COPYING ${dest_dir}/opus
cp ${build_dir}/libjpeg-turbo-prefix/src/libjpeg-turbo/LICENSE.md ${dest_dir}/libjpeg-turbo
cp ${build_dir}/libwebp-prefix/src/libwebp/COPYING ${dest_dir}/libwebp
cp ${build_dir}/libpng-prefix/src/libpng/LICENSE ${dest_dir}/libpng
cp ${build_dir}/libffi-prefix/src/libffi/LICENSE ${dest_dir}/libffi
cp ${build_dir}/libsndfile-prefix/src/libsndfile/COPYING ${dest_dir}/libsndfile

xsystem4_deps_dir=${build_dir}/xsystem4-prefix/src/xsystem4-build/_deps
cp ${xsystem4_deps_dir}/cglm-src/LICENSE ${dest_dir}/cglm
cp ${xsystem4_deps_dir}/sdl-src/LICENSE.txt ${dest_dir}/SDL
cp ${xsystem4_deps_dir}/freetype-src/docs/GPLv2.TXT ${dest_dir}/freetype
cp xsystem4/COPYING ${dest_dir}/xsystem4
cp xsystem4/subprojects/libsys4/COPYING ${dest_dir}/libsys4
cp xsystem4/fonts/VL-Gothic-Regular.license ${dest_dir}/VL-Gothic-Regular
cp xsystem4/fonts/HanaMinA.license ${dest_dir}/HanaMinA
