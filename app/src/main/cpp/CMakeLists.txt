# VoidTerm - Native Engine CMakeLists.txt
# Provides: PTY handling, process management, escape code parsing, I/O buffering
# Developer: Asotn | https://github.com/Asotn

cmake_minimum_required(VERSION 3.22.1)
project("voidterm_native" VERSION 26.2 LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags
set(CMAKE_C_FLAGS_RELEASE   "-O2 -DNDEBUG -fvisibility=hidden")
set(CMAKE_CXX_FLAGS_RELEASE "-O2 -DNDEBUG -fvisibility=hidden")

# -------------------------------------------------------------------------
# voidterm_native shared library
# -------------------------------------------------------------------------
add_library(
    voidterm_native
    SHARED

    # PTY / process core
    terminal/pty_manager.c
    terminal/process_runner.c
    terminal/io_buffer.c

    # ANSI escape code parser (VT100/VT220/xterm)
    terminal/escape_parser.c
    terminal/vt100.c

    # Shell utilities
    shell/command_tokenizer.c
    shell/env_manager.c
    shell/alias_engine.c
    shell/history_manager.c
    shell/shell_quote.c

    # Package engine helpers (dpkg query, dependency resolver assist)
    package/dpkg_helper.c
    package/apt_wrapper.c
    package/repo_manager.c

    # File system helpers
    fs/fs_utils.c
    fs/path_resolver.c
    fs/permission_helper.c

    # Networking helpers
    net/http_client.c
    net/progress_tracker.c

    # Crypto / checksum
    crypto/sha256.c
    crypto/md5.c
    crypto/pgp_verify.c

    # JNI bridge
    jni/jni_bridge.cpp
    jni/terminal_jni.cpp
    jni/package_jni.cpp
    jni/fs_jni.cpp
)

# Android system libraries
find_library(log-lib   log)
find_library(android-lib android)

target_link_libraries(
    voidterm_native
    ${log-lib}
    ${android-lib}
)

target_include_directories(voidterm_native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/terminal
    ${CMAKE_CURRENT_SOURCE_DIR}/shell
    ${CMAKE_CURRENT_SOURCE_DIR}/package
    ${CMAKE_CURRENT_SOURCE_DIR}/fs
    ${CMAKE_CURRENT_SOURCE_DIR}/net
    ${CMAKE_CURRENT_SOURCE_DIR}/crypto
    ${CMAKE_CURRENT_SOURCE_DIR}/jni
)

# Compile definitions
target_compile_definitions(voidterm_native PRIVATE
    ANDROID_BUILD=1
    KALI_REPO_URL="https://http.kali.org/kali"
    KALI_DIST="kali-rolling"
    APP_VERSION="26.2"
)
