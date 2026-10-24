cmake_minimum_required(VERSION 3.22.1)

project(rud_llama_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(rud_llama_jni SHARED
    rud_llama_jni.cpp
)

find_library(log-lib log)

target_include_directories(rud_llama_jni PRIVATE
    ${CMAKE_ANDROID_NDK}/sources/android/native_app_glue
)

target_link_libraries(rud_llama_jni
    PRIVATE
    ${log-lib}
    android
)
