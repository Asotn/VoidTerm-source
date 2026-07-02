/*
 * VoidTerm - Filesystem JNI
 * JNI bindings for fs_utils, path_resolver, permission_helper.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include <jni.h>
#include <string>
#include <android/log.h>
#include "../fs/fs_utils.h"
#include "../fs/path_resolver.h"
#include "../fs/permission_helper.h"
#include "../crypto/sha256.h"

#define LOG_TAG "VoidTerm-FsJNI"

static std::string jstr2std(JNIEnv *env, jstring js) {
    if (!js) return "";
    const char *c = env->GetStringUTFChars(js, nullptr);
    std::string s(c);
    env->ReleaseStringUTFChars(js, c);
    return s;
}

// =========================================================================
// Path resolver
// =========================================================================

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativePathResolverInit(
        JNIEnv *env, jclass, jstring rootfs, jstring home, jstring sdcard) {
    path_resolver_init(jstr2std(env, rootfs).c_str(),
                       jstr2std(env, home).c_str(),
                       jstr2std(env, sdcard).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeGuestToHost(
        JNIEnv *env, jclass, jstring guestPath) {
    char out[1024] = {0};
    path_guest_to_host(jstr2std(env, guestPath).c_str(), out, sizeof(out));
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativePathJoin(
        JNIEnv *env, jclass, jstring base, jstring rel) {
    char out[1024] = {0};
    path_join(jstr2std(env, base).c_str(),
              jstr2std(env, rel).c_str(),
              out, sizeof(out));
    return env->NewStringUTF(out);
}

// =========================================================================
// FS utils
// =========================================================================

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeExists(
        JNIEnv *env, jclass, jstring path) {
    return (jboolean)fs_exists(jstr2std(env, path).c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeFileSize(
        JNIEnv *env, jclass, jstring path) {
    return (jlong)fs_file_size(jstr2std(env, path).c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeFreeSpace(
        JNIEnv *env, jclass, jstring path) {
    return (jlong)fs_get_free_space(jstr2std(env, path).c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeMkdirs(
        JNIEnv *env, jclass, jstring path) {
    return (jboolean)(fs_mkdirs(jstr2std(env, path).c_str(), 0755) == 0);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeDeleteFile(
        JNIEnv *env, jclass, jstring path) {
    return (jboolean)(fs_delete_file(jstr2std(env, path).c_str()) == 0);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeDeleteDirRecursive(
        JNIEnv *env, jclass, jstring path) {
    return (jboolean)(fs_delete_dir_recursive(jstr2std(env, path).c_str()) == 0);
}

// =========================================================================
// Permissions
// =========================================================================

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeCanRead(
        JNIEnv *env, jclass, jstring path) {
    return (jboolean)perm_can_read(jstr2std(env, path).c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeCanWrite(
        JNIEnv *env, jclass, jstring path) {
    return (jboolean)perm_can_write(jstr2std(env, path).c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeSdcardReadable(
        JNIEnv *, jclass) {
    return (jboolean)perm_check_sdcard();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeSdcardWritable(
        JNIEnv *, jclass) {
    return (jboolean)perm_check_sdcard_write();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeGetModeString(
        JNIEnv *env, jclass, jstring path) {
    char mode[11] = {0};
    perm_get_mode_string(jstr2std(env, path).c_str(), mode);
    return env->NewStringUTF(mode);
}

// =========================================================================
// SHA256
// =========================================================================

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeSha256File(
        JNIEnv *env, jclass, jstring path) {
    char hex[65] = {0};
    sha256_file(jstr2std(env, path).c_str(), hex);
    return env->NewStringUTF(hex);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_utils_NativeFs_nativeSha256Verify(
        JNIEnv *env, jclass, jstring path, jstring expected) {
    int r = sha256_verify_file(jstr2std(env, path).c_str(),
                                jstr2std(env, expected).c_str());
    return (jboolean)(r == 0);
}
