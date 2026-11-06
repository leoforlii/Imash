cmake_minimum_required(VERSION 3.22.1)
project(imash_audio C)

# LAME 3.100 (LGPL): solo l'encoder, sorgenti non modificati in lame/.
file(GLOB LAME_SRC CONFIGURE_DEPENDS lame/libmp3lame/*.c)
add_library(mp3lame STATIC ${LAME_SRC})
target_include_directories(mp3lame
    PUBLIC lame/include
    PRIVATE lame/libmp3lame)
target_compile_definitions(mp3lame PRIVATE
    STDC_HEADERS HAVE_STDINT_H HAVE_LIMITS_H
    # Senza config.h di autotools questo tipo non viene definito.
    ieee754_float32_t=float)
target_compile_options(mp3lame PRIVATE -O3 -w)

# Ponte JNI usato da com.imash.app.audio.Mp3Encoder.
add_library(imash_audio SHARED lame_jni.c)
target_link_libraries(imash_audio PRIVATE mp3lame m)
