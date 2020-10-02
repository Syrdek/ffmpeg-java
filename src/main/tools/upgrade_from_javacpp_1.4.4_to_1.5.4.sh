#!/bin/bash

find src -name '*.java' | xargs -i sed -i -r \
 -e 's/import org.bytedeco.javacpp.([a-z]+);/import org.bytedeco.ffmpeg.global.\1;/g' \
 -e 's/import org.bytedeco.javacpp/import org.bytedeco.ffmpeg/g' \
 -e 's/import org.bytedeco.ffmpeg.(Loader|.*Pointer);/import org.bytedeco.javacpp.\1;/g' {}
