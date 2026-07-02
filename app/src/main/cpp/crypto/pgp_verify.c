/*
 * VoidTerm - PGP Verification
 * Verifies Kali Linux Release file PGP signatures.
 * Wraps gpgv via proot environment.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "pgp_verify.h"
#include "../terminal/process_runner.h"
#include "../shell/shell_quote.h"
#include <string.h>
#include <stdio.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-PGP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// -------------------------------------------------------------------------
// pgp_verify_release
// Verifies the Kali InRelease file signature using gpgv inside proot.
// Returns 0 if valid, -1 on failure.
// -------------------------------------------------------------------------
int pgp_verify_release(const char *proot_bin,
                        const char *rootfs,
                        const char *inrelease_path) {
    if (!proot_bin || !rootfs || !inrelease_path) return -1;

    // Build gpgv command inside proot. inrelease_path is quoted because it
    // can be influenced by the configured mirror/repo path; without quoting
    // a crafted path could inject additional shell commands.
    char q_path[1200];
    if (shell_quote(inrelease_path, q_path, sizeof(q_path)) != 0) {
        LOGE("pgp_verify_release: path too long/unsafe to quote");
        return -1;
    }

    char cmd[1400];
    snprintf(cmd, sizeof(cmd),
        "gpgv --keyring /usr/share/keyrings/kali-archive-keyring.gpg %s",
        q_path);

    char out[2048] = {0};
    int ret = process_run_proot(proot_bin, rootfs, cmd, out, sizeof(out));

    if (ret == 0) {
        LOGI("PGP verification OK: %s", inrelease_path);
    } else {
        LOGE("PGP verification FAILED: %s\n%s", inrelease_path, out);
    }

    return ret;
}

// -------------------------------------------------------------------------
// pgp_import_kali_key
// Imports the Kali archive key inside the proot environment.
// -------------------------------------------------------------------------
int pgp_import_kali_key(const char *proot_bin, const char *rootfs) {
    if (!proot_bin || !rootfs) return -1;

    const char *cmd = "apt-get install -y kali-archive-keyring";
    char out[4096] = {0};
    int ret = process_run_proot(proot_bin, rootfs, cmd, out, sizeof(out));
    LOGI("Key import result: %d", ret);
    return ret;
}

// -------------------------------------------------------------------------
// pgp_check_available
// Checks if gpgv is available in the proot environment.
// -------------------------------------------------------------------------
int pgp_check_available(const char *proot_bin, const char *rootfs) {
    char out[64] = {0};
    int ret = process_run_proot(proot_bin, rootfs,
                                 "which gpgv", out, sizeof(out));
    return ret == 0 && strlen(out) > 0;
}
