# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#设置变量SRC_DIR为lamemp3的所在路径
set(SRC_DIR src/main/cpp/lame)

#指定头文件所在，可以多次调用，指定多个路径
include_directories(src/main/cpp/lame)

#添加自自定义的so库时，有两种方式，一种添加一个目录，一种一个个文件添加

#设定一个目录
aux_source_directory(src/main/cpp/lame SRC_LIST)


add_library( # Sets the name of the library.
        mp3lame

        # Sets the library as a shared library.
        SHARED


        ${SRC_LIST}
        # Provides a relative path to your source file(s).
        src/main/cpp/lame_util.c
        #        src/main/jni/lame/bitstream.c
        #src/main/jni/lame/fft.c
        #src/main/jni/lame/id3tag.c
        #src/main/jni/lame/mpglib_interface.c
        #src/main/jni/lame/presets.c
        #src/main/jni/lame/quantize.c
        #src/main/jni/lame/reservoir.c
        #src/main/jni/lame/tables.c
        #src/main/jni/lame/util.c
        #src/main/jni/lame/VbrTag.c
        #src/main/jni/lame/encoder.c
        #src/main/jni/lame/gain_analysis.c
        #src/main/jni/lame/lame.c
        #src/main/jni/lame/newmdct.c
        #src/main/jni/lame/psymodel.c
        #src/main/jni/lame/quantize_pvt.c
        #src/main/jni/lame/set_get.c
        #src/main/jni/lame/takehiro.c
        #src/main/jni/lame/vbrquantize.c
        #src/main/jni/lame/version.c
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.


