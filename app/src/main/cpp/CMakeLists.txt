# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
include_directories( ncnn-android-vulkan-lib/include/ncnn)
add_library(ncnn STATIC IMPORTED)

set_target_properties(
        ncnn
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan-lib/${ANDROID_ABI}/libncnn.a)

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)
# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS}  -fopenmp")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -fopenmp")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS}   -fopenmp")

if(DEFINED ANDROID_NDK_MAJOR AND ${ANDROID_NDK_MAJOR} GREATER 20)
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static-openmp")
endif()

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fomit-frame-pointer -fstrict-aliasing -ffast-math")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fomit-frame-pointer -fstrict-aliasing -ffast-math")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -fvisibility-inlines-hidden")

# disable rtti and exceptions
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -fno-exceptions")




find_library(log-lib log)
find_library(android-lib android)
find_library(vulkan-lib vulkan)
find_library(jnigraphics-lib jnigraphics)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib
        ncnn


        ${log-lib}
        ${vulkan-lib}
        ${android-lib}
        ${jnigraphics-lib}
        )