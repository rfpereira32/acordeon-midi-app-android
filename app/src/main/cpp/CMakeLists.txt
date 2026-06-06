#
# Copyright (c) 2024 Robson Martins
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

cmake_minimum_required(VERSION 3.4.1)
project(android-midi-synth)

# Where the fluidsynth library is located.
set(fluidsynth_HEADERS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/fluidsynth)
set(fluidsynth_LIBS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/..)

# fluidsynth lib binaries
add_library(libc++_shared SHARED IMPORTED)
set_target_properties(libc++_shared PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/libc++_shared.so)

add_library(libfluidsynth SHARED IMPORTED)
set_target_properties(libfluidsynth PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/libfluidsynth.so)

add_library(libgio-2.0 SHARED IMPORTED)
set_target_properties(libgio-2.0 PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/libgio-2.0.so)

add_library(libglib-2.0 SHARED IMPORTED)
set_target_properties(libglib-2.0 PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/libglib-2.0.so)

add_library(libgmodule-2.0 SHARED IMPORTED)
set_target_properties(libgmodule-2.0 PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/libgmodule-2.0.so)

add_library(libgobject-2.0 SHARED IMPORTED)
set_target_properties(libgobject-2.0 PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/libgobject-2.0.so)

add_library(libgthread-2.0 SHARED IMPORTED)
set_target_properties(libgthread-2.0 PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/libgthread-2.0.so)

add_library(liboboe SHARED IMPORTED)
set_target_properties(liboboe PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/liboboe.so)

add_library(libsndfile SHARED IMPORTED)
set_target_properties(libsndfile PROPERTIES IMPORTED_LOCATION ${fluidsynth_LIBS_DIR}/jniLibs/${ANDROID_ABI}/libsndfile.so)

find_package(OpenMP REQUIRED)

# Native Library that will be called directly from JAVA
add_library(synth-lib SHARED
		MidiManager.cpp
		SynthManager.cpp
)

# Include fluidsynth header directory
#target_include_directories(synth-lib PRIVATE ${fluidsynth_DIR}/include)
target_include_directories(synth-lib PRIVATE ${fluidsynth_HEADERS_DIR}/include)
#	include_directories(${fluidsynth_DIR}/include)

# Link everything (native lib should be the first element in the list)
target_link_libraries(
		synth-lib
		libc++_shared
        libgio-2.0
        libglib-2.0
        libgmodule-2.0
        libgobject-2.0
        libgthread-2.0
        liboboe
        libsndfile
        libfluidsynth
        OpenMP::OpenMP_CXX
		amidi
		log
)
