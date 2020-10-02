#!/bin/bash

find src -name '*.java' | xargs -i sed -i \
 -e 's/import org.bytedeco.ffmpeg.global/import org.bytedeco.javacpp/g' \
 -e 's/import org.bytedeco.ffmpeg/import org.bytedeco.javacpp/g' {}
