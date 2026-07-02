/*
 * VoidTerm - Terminal JNI Extensions
 * JNI bindings for VT100 emulator, history, alias, env managers.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include <jni.h>
#include <string>
#include <android/log.h>
#include "../terminal/vt100.h"
#include "../shell/history_manager.h"
#include "../shell/alias_engine.h"
#include "../shell/env_manager.h"
#include "../shell/command_tokenizer.h"

#define LOG_TAG "VoidTerm-TermJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static vt100_t    g_vt100;
static history_t  g_history;
static bool       g_vt100_init    = false;
static bool       g_history_init  = false;

// =========================================================================
// VT100
// =========================================================================

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeVt100Init(
        JNIEnv *, jobject, jint cols, jint rows) {
    if (g_vt100_init) vt100_destroy(&g_vt100);
    int r = vt100_init(&g_vt100, cols, rows);
    g_vt100_init = (r == 0);
    return r;
}

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeVt100Feed(
        JNIEnv *env, jobject, jbyteArray data) {
    if (!g_vt100_init || !data) return;
    jsize len = env->GetArrayLength(data);
    jbyte *buf = env->GetByteArrayElements(data, nullptr);
    vt100_feed(&g_vt100, (const char *)buf, (size_t)len);
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeVt100Resize(
        JNIEnv *, jobject, jint cols, jint rows) {
    if (!g_vt100_init) return -1;
    return vt100_resize(&g_vt100, cols, rows);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeVt100GetCursorRow(
        JNIEnv *, jobject) {
    return g_vt100_init ? g_vt100.cursor_row : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeVt100GetCursorCol(
        JNIEnv *, jobject) {
    return g_vt100_init ? g_vt100.cursor_col : 0;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeVt100IsDirty(
        JNIEnv *, jobject) {
    return g_vt100_init && g_vt100.dirty;
}

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeVt100ClearDirty(
        JNIEnv *, jobject) {
    if (g_vt100_init) g_vt100.dirty = 0;
}

// =========================================================================
// History
// =========================================================================

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeHistoryInit(
        JNIEnv *env, jobject, jint maxEntries, jstring historyFile) {
    const char *path = historyFile
        ? env->GetStringUTFChars(historyFile, nullptr)
        : nullptr;
    int r = history_init(&g_history, maxEntries, path);
    if (path) env->ReleaseStringUTFChars(historyFile, path);
    g_history_init = (r == 0);
    return r;
}

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeHistoryAdd(
        JNIEnv *env, jobject, jstring cmd) {
    if (!g_history_init || !cmd) return;
    const char *c = env->GetStringUTFChars(cmd, nullptr);
    history_add(&g_history, c);
    env->ReleaseStringUTFChars(cmd, c);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeHistoryPrev(
        JNIEnv *env, jobject) {
    if (!g_history_init) return env->NewStringUTF("");
    const char *s = history_prev(&g_history);
    return env->NewStringUTF(s ? s : "");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeHistoryNext(
        JNIEnv *env, jobject) {
    if (!g_history_init) return env->NewStringUTF("");
    const char *s = history_next(&g_history);
    return env->NewStringUTF(s ? s : "");
}

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeHistorySave(
        JNIEnv *, jobject) {
    if (g_history_init) history_save(&g_history);
}

// =========================================================================
// Alias
// =========================================================================

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeAliasInit(
        JNIEnv *, jobject) {
    alias_init();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeAliasExpand(
        JNIEnv *env, jobject, jstring cmd) {
    if (!cmd) return env->NewStringUTF("");
    const char *c = env->GetStringUTFChars(cmd, nullptr);
    char out[4096];
    int expanded = alias_expand(c, out, sizeof(out));
    env->ReleaseStringUTFChars(cmd, c);
    return env->NewStringUTF(expanded ? out : c);
}

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeAliasSet(
        JNIEnv *env, jobject, jstring name, jstring expansion) {
    if (!name || !expansion) return;
    const char *n = env->GetStringUTFChars(name, nullptr);
    const char *e = env->GetStringUTFChars(expansion, nullptr);
    alias_set(n, e);
    env->ReleaseStringUTFChars(name, n);
    env->ReleaseStringUTFChars(expansion, e);
}

// =========================================================================
// Command tokenizer
// =========================================================================

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_NativeTerminal_nativeGetFirstWord(
        JNIEnv *env, jobject, jstring cmd) {
    if (!cmd) return env->NewStringUTF("");
    const char *c = env->GetStringUTFChars(cmd, nullptr);
    token_list_t tl;
    tokenize(c, &tl);
    const char *word = token_list_first_word(&tl);
    jstring result = env->NewStringUTF(word);
    token_list_destroy(&tl);
    env->ReleaseStringUTFChars(cmd, c);
    return result;
}
