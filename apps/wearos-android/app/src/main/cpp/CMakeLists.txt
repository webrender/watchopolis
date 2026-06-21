cmake_minimum_required(VERSION 3.22.1)
project(micropolis_wear LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Locate the shared C++ engine source in the monorepo (built in-tree, not copied).
get_filename_component(ENGINE_DIR
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../packages/micropolis-engine/src"
    ABSOLUTE)

file(GLOB ENGINE_SOURCES "${ENGINE_DIR}/*.cpp")
# Exclude the Emscripten-only translation units; this app supplies its own JNI
# bridge and a native Callback implementation:
#   - emscripten.cpp : embind JS bindings
#   - callback.cpp   : ConsoleCallback, whose bodies are all EM_ASM_ JS calls
list(FILTER ENGINE_SOURCES EXCLUDE REGEX "(emscripten|callback)\\.cpp$")

add_library(micropolis SHARED
    micropolis_jni.cpp
    ${ENGINE_SOURCES})

target_include_directories(micropolis PRIVATE ${ENGINE_DIR})

# The engine targets Emscripten/WASM; silence its warnings under the NDK toolchain.
target_compile_options(micropolis PRIVATE -w)

find_library(log-lib log)
target_link_libraries(micropolis ${log-lib})
