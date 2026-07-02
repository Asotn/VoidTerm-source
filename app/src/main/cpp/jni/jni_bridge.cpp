/*
 * VoidTerm - JNI Bridge
 * Connects Java layer to the C/C++ terminal and package engine.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include <jni.h>
#include <string>
#include <cstring>
#include <cstdlib>
#include <android/log.h>
#include "../terminal/pty_manager.h"

#define LOG_TAG "VoidTerm-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Utility: convert jstring to std::string
static std::string jstr(JNIEnv *env, jstring js) {
    if (!js) return "";
    const char *c = env->GetStringUTFChars(js, nullptr);
    std::string s(c);
    env->ReleaseStringUTFChars(js, c);
    return s;
}

// =========================================================================
// PTY / Terminal JNI
// =========================================================================

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeInit(JNIEnv *, jobject) {
    return pty_manager_init();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeOpenSession(
        JNIEnv *env, jobject,
        jstring shellPath,
        jobjectArray argvArr,
        jobjectArray envpArr,
        jstring cwd,
        jint cols, jint rows) {

    std::string shell = jstr(env, shellPath);
    std::string wd    = jstr(env, cwd);

    // Build argv
    int argc = argvArr ? env->GetArrayLength(argvArr) : 0;
    const char **argv = new const char *[argc + 1];
    std::string *argvStrs = new std::string[argc];
    for (int i = 0; i < argc; i++) {
        argvStrs[i] = jstr(env, (jstring)env->GetObjectArrayElement(argvArr, i));
        argv[i] = argvStrs[i].c_str();
    }
    argv[argc] = nullptr;

    // Build envp
    int envc = envpArr ? env->GetArrayLength(envpArr) : 0;
    const char **envp = new const char *[envc + 1];
    std::string *envpStrs = new std::string[envc];
    for (int i = 0; i < envc; i++) {
        envpStrs[i] = jstr(env, (jstring)env->GetObjectArrayElement(envpArr, i));
        envp[i] = envpStrs[i].c_str();
    }
    envp[envc] = nullptr;

    int session = pty_open_session(
        shell.c_str(), argv, envc > 0 ? envp : nullptr,
        wd.empty() ? nullptr : wd.c_str(),
        cols, rows
    );

    delete[] argv;
    delete[] argvStrs;
    delete[] envp;
    delete[] envpStrs;

    return session;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeWrite(
        JNIEnv *env, jobject, jint session, jbyteArray data) {
    if (!data) return -1;
    jsize len = env->GetArrayLength(data);
    jbyte *buf = env->GetByteArrayElements(data, nullptr);
    ssize_t n = pty_write(session, (const char *)buf, (size_t)len);
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
    return (jint)n;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeRead(
        JNIEnv *env, jobject, jint session, jint maxLen) {
    char *buf = new char[maxLen];
    ssize_t n = pty_read(session, buf, (size_t)maxLen);
    if (n <= 0) {
        delete[] buf;
        return nullptr;
    }
    jbyteArray result = env->NewByteArray((jsize)n);
    env->SetByteArrayRegion(result, 0, (jsize)n, (jbyte *)buf);
    delete[] buf;
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeResize(
        JNIEnv *, jobject, jint session, jint cols, jint rows) {
    return pty_resize(session, cols, rows);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeClose(
        JNIEnv *, jobject, jint session) {
    return pty_close_session(session);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeIsAlive(
        JNIEnv *, jobject, jint session) {
    return (jboolean)pty_is_alive(session);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeSendSignal(
        JNIEnv *, jobject, jint session, jint sig) {
    return pty_send_signal(session, sig);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeGetMasterFd(
        JNIEnv *, jobject, jint session) {
    return pty_get_master_fd(session);
}

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeDestroy(
        JNIEnv *, jobject) {
    pty_manager_destroy();
}

// =========================================================================
// Version info
// =========================================================================

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeGetVersion(
        JNIEnv *env, jobject) {
    return env->NewStringUTF("VoidTerm Native Engine v26.2");
}
