/*
 * VoidTerm - Package JNI
 * JNI bindings for the APT wrapper and dpkg helper.
 * Allows Java to query and control package operations.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include <jni.h>
#include <string>
#include <android/log.h>
#include "../package/apt_wrapper.h"
#include "../package/dpkg_helper.h"
#include "../package/repo_manager.h"

#define LOG_TAG "VoidTerm-PkgJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::string jstr2std(JNIEnv *env, jstring js) {
    if (!js) return "";
    const char *c = env->GetStringUTFChars(js, nullptr);
    std::string s(c);
    env->ReleaseStringUTFChars(js, c);
    return s;
}

// =========================================================================
// APT
// =========================================================================

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeAptInit(
        JNIEnv *env, jobject, jstring proofBin, jstring rootfs, jstring mirror) {
    apt_init(jstr2std(env, proofBin).c_str(),
             jstr2std(env, rootfs).c_str(),
             jstr2std(env, mirror).c_str());
    dpkg_init(jstr2std(env, proofBin).c_str(),
              jstr2std(env, rootfs).c_str());
    repo_manager_init(jstr2std(env, proofBin).c_str(),
                      jstr2std(env, rootfs).c_str(), "arm64");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeAptUpdate(
        JNIEnv *env, jobject) {
    char out[65536] = {0};
    apt_update(out, sizeof(out));
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeAptInstall(
        JNIEnv *env, jobject, jstring pkgList) {
    char out[65536] = {0};
    apt_install(jstr2std(env, pkgList).c_str(), out, sizeof(out));
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeAptRemove(
        JNIEnv *env, jobject, jstring pkgList, jboolean purge) {
    char out[65536] = {0};
    apt_remove(jstr2std(env, pkgList).c_str(), purge ? 1 : 0, out, sizeof(out));
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeAptSearch(
        JNIEnv *env, jobject, jstring query) {
    char out[65536] = {0};
    apt_search(jstr2std(env, query).c_str(), out, sizeof(out));
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeAptShow(
        JNIEnv *env, jobject, jstring pkg) {
    char out[16384] = {0};
    apt_show(jstr2std(env, pkg).c_str(), out, sizeof(out));
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeAptUpgrade(
        JNIEnv *env, jobject, jboolean distUpgrade) {
    char out[65536] = {0};
    apt_upgrade(distUpgrade ? 1 : 0, out, sizeof(out));
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeAptFixBroken(
        JNIEnv *env, jobject) {
    char out[16384] = {0};
    apt_fix_broken(out, sizeof(out));
    return env->NewStringUTF(out);
}

// =========================================================================
// dpkg
// =========================================================================

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeDpkgIsInstalled(
        JNIEnv *env, jobject, jstring pkg) {
    return (jboolean)dpkg_is_installed(jstr2std(env, pkg).c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeDpkgGetVersion(
        JNIEnv *env, jobject, jstring pkg) {
    char ver[256] = {0};
    dpkg_get_version(jstr2std(env, pkg).c_str(), ver, sizeof(ver));
    return env->NewStringUTF(ver);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeDpkgListInstalled(
        JNIEnv *env, jobject) {
    char out[131072] = {0};
    dpkg_list_installed(out, sizeof(out));
    return env->NewStringUTF(out);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeDpkgInstalledCount(
        JNIEnv *, jobject) {
    return dpkg_get_installed_count();
}

// =========================================================================
// Repo
// =========================================================================

extern "C" JNIEXPORT jboolean JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeRepoCheckConnectivity(
        JNIEnv *env, jobject, jstring mirror) {
    int r = repo_check_connectivity(jstr2std(env, mirror).c_str());
    return (jboolean)(r == 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_asotn_voidterm_engine_PackageEngine_nativeRepoWriteSourcesList(
        JNIEnv *, jobject) {
    repo_write_sources_list();
}
