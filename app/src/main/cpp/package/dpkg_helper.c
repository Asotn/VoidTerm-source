/*
 * VoidTerm - dpkg Helper
 * Queries the dpkg package database inside the Kali rootfs.
 * Provides package status, version, installed file list, etc.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "dpkg_helper.h"
#include "../terminal/process_runner.h"
#include "../fs/fs_utils.h"
#include "../shell/shell_quote.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-dpkg"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static char g_rootfs[512]   = {0};
static char g_proot_bin[512] = {0};

// -------------------------------------------------------------------------
// dpkg_init
// -------------------------------------------------------------------------
void dpkg_init(const char *proot_bin, const char *rootfs) {
    if (proot_bin) strncpy(g_proot_bin, proot_bin, sizeof(g_proot_bin) - 1);
    if (rootfs)    strncpy(g_rootfs,    rootfs,    sizeof(g_rootfs)    - 1);
}

// -------------------------------------------------------------------------
// dpkg_run
// Runs a dpkg command inside proot and captures output.
// -------------------------------------------------------------------------
static int dpkg_run(const char *args, char *out, size_t out_size) {
    char cmd[1024];
    snprintf(cmd, sizeof(cmd), "dpkg %s", args);
    return process_run_proot(g_proot_bin, g_rootfs, cmd, out, out_size);
}

// -------------------------------------------------------------------------
// dpkg_is_installed
// Returns 1 if package is installed, 0 otherwise.
// -------------------------------------------------------------------------
int dpkg_is_installed(const char *pkg_name) {
    if (!pkg_name) return 0;
    if (!shell_is_safe_token(pkg_name)) return 0;
    char args[512];
    snprintf(args, sizeof(args), "-l %s 2>/dev/null | grep -q '^ii'", pkg_name);
    char out[64] = {0};
    int ret = dpkg_run(args, out, sizeof(out));
    return ret == 0;
}

// -------------------------------------------------------------------------
// dpkg_get_version
// Writes the installed version of pkg_name into version_buf.
// Returns 0 on success, -1 if not installed.
// -------------------------------------------------------------------------
int dpkg_get_version(const char *pkg_name, char *version_buf, size_t buf_size) {
    if (!pkg_name || !version_buf) return -1;
    if (!shell_is_safe_token(pkg_name)) return -1;
    char args[512];
    snprintf(args, sizeof(args), "-s %s 2>/dev/null | grep '^Version:' | cut -d' ' -f2", pkg_name);
    char out[256] = {0};
    int ret = dpkg_run(args, out, sizeof(out));
    if (ret != 0 || strlen(out) == 0) return -1;

    // Strip trailing newline
    size_t len = strlen(out);
    while (len > 0 && (out[len-1] == '\n' || out[len-1] == '\r')) out[--len] = '\0';

    strncpy(version_buf, out, buf_size - 1);
    version_buf[buf_size - 1] = '\0';
    return 0;
}

// -------------------------------------------------------------------------
// dpkg_get_status
// Returns the dpkg status string for a package.
// -------------------------------------------------------------------------
int dpkg_get_status(const char *pkg_name, dpkg_status_t *status) {
    if (!pkg_name || !status) return -1;
    if (!shell_is_safe_token(pkg_name)) return -1;
    *status = DPKG_STATUS_UNKNOWN;

    char args[512];
    snprintf(args, sizeof(args),
        "-s %s 2>/dev/null | grep '^Status:' | awk '{print $4}'", pkg_name);
    char out[64] = {0};
    int ret = dpkg_run(args, out, sizeof(out));
    if (ret != 0) return -1;

    if (strstr(out, "installed"))   *status = DPKG_STATUS_INSTALLED;
    else if (strstr(out, "config")) *status = DPKG_STATUS_CONFIG_FILES;
    else if (strstr(out, "not-installed")) *status = DPKG_STATUS_NOT_INSTALLED;
    else if (strstr(out, "unpacked"))     *status = DPKG_STATUS_UNPACKED;
    else if (strstr(out, "half-configured")) *status = DPKG_STATUS_HALF_CONFIGURED;
    else if (strstr(out, "half-installed"))  *status = DPKG_STATUS_HALF_INSTALLED;

    return 0;
}

// -------------------------------------------------------------------------
// dpkg_list_installed
// Writes a newline-separated list of installed packages into buf.
// -------------------------------------------------------------------------
int dpkg_list_installed(char *buf, size_t buf_size) {
    if (!buf) return -1;
    const char *args = "-l 2>/dev/null | awk '/^ii/{print $2}'";
    return dpkg_run(args, buf, buf_size);
}

// -------------------------------------------------------------------------
// dpkg_get_installed_count
// Returns the number of installed packages.
// -------------------------------------------------------------------------
int dpkg_get_installed_count(void) {
    char out[16] = {0};
    const char *args = "-l 2>/dev/null | grep -c '^ii'";
    int ret = dpkg_run(args, out, sizeof(out));
    if (ret != 0) return -1;
    return atoi(out);
}

// -------------------------------------------------------------------------
// dpkg_get_files
// Lists files owned by a package.
// -------------------------------------------------------------------------
int dpkg_get_files(const char *pkg_name, char *buf, size_t buf_size) {
    if (!pkg_name || !buf) return -1;
    if (!shell_is_safe_token(pkg_name)) return -1;
    char args[512];
    snprintf(args, sizeof(args), "-L %s 2>/dev/null", pkg_name);
    return dpkg_run(args, buf, buf_size);
}

// -------------------------------------------------------------------------
// dpkg_reconfigure
// Runs dpkg-reconfigure for a package.
// -------------------------------------------------------------------------
int dpkg_reconfigure(const char *pkg_name, char *out, size_t out_size) {
    if (!pkg_name) return -1;
    if (!shell_is_safe_token(pkg_name)) return -1;
    char cmd[512];
    snprintf(cmd, sizeof(cmd),
        "DEBIAN_FRONTEND=noninteractive dpkg-reconfigure %s", pkg_name);
    return process_run_proot(g_proot_bin, g_rootfs, cmd, out, out_size);
}

// -------------------------------------------------------------------------
// dpkg_fix_broken
// Runs dpkg --configure -a to fix broken installs.
// -------------------------------------------------------------------------
int dpkg_fix_broken(char *out, size_t out_size) {
    return process_run_proot(g_proot_bin, g_rootfs,
        "dpkg --configure -a 2>&1", out, out_size);
}

// -------------------------------------------------------------------------
// dpkg_status_to_string
// -------------------------------------------------------------------------
const char *dpkg_status_to_string(dpkg_status_t status) {
    switch (status) {
    case DPKG_STATUS_INSTALLED:        return "installed";
    case DPKG_STATUS_NOT_INSTALLED:    return "not-installed";
    case DPKG_STATUS_CONFIG_FILES:     return "config-files";
    case DPKG_STATUS_UNPACKED:         return "unpacked";
    case DPKG_STATUS_HALF_CONFIGURED:  return "half-configured";
    case DPKG_STATUS_HALF_INSTALLED:   return "half-installed";
    default:                           return "unknown";
    }
}
