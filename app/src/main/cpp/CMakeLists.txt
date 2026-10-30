cmake_minimum_required(VERSION 3.22.1)
project(rud_llama_jni LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# O llama.cpp precisa SEMPRE ser compilado otimizado. Sem isso (build "Debug",
# -O0) a geração fica dezenas de vezes mais lenta e o app parece travado em
# "Pensando...".
if(NOT CMAKE_BUILD_TYPE OR CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
endif()

include(FetchContent)

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
# Helio G85 (Redmi Note 13 4G): Cortex-A75 + A55 => ARMv8.2-A com dotprod.
set(GGML_CPU_ARM_ARCH "armv8.2-a+dotprod+fp16" CACHE STRING "" FORCE)

# Versão FIXA do llama.cpp. Antes era "master", o que fazia o build quebrar
# sozinho sempre que a API do llama.cpp mudava.
FetchContent_Declare(
    llama_cpp
    URL https://github.com/ggml-org/llama.cpp/archive/refs/tags/v0.7.0.tar.gz
)

FetchContent_MakeAvailable(llama_cpp)

add_library(rud_llama_jni SHARED
    rud_llama_jni.cpp
)

target_compile_options(rud_llama_jni PRIVATE -O3)

find_library(log-lib log)

target_link_libraries(rud_llama_jni
    PRIVATE llama ${log-lib} android
)
