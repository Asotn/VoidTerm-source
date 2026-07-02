/*
 * VoidTerm - Path Resolver
 * Translates guest paths (inside Kali rootfs) to host paths and vice versa.
 * Also handles Android-specific path expansions (~/  /sdcard/ etc.)
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "path_resolver.h"
#include "fs_utils.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-Path"

static char g_rootfs[512]  = {0};
static char g_home[512]    = {0};
static char g_sdcard[512]  = {0};

// -------------------------------------------------------------------------
// path_resolver_init
// -------------------------------------------------------------------------
void path_resolver_init(const char *rootfs, const char *home, const char *sdcard) {
    if (rootfs) strncpy(g_rootfs,  rootfs,  sizeof(g_rootfs)  - 1);
    if (home)   strncpy(g_home,    home,    sizeof(g_home)    - 1);
    if (sdcard) strncpy(g_sdcard,  sdcard,  sizeof(g_sdcard)  - 1);
}

// -------------------------------------------------------------------------
// path_guest_to_host
// Converts an in-rootfs path like /root/.bashrc
// to the host path like /data/.../kali-rootfs/root/.bashrc
//
// SECURITY: the guest path is untrusted (it can come from terminal input,
// package metadata, or other guest-controlled sources). Without
// normalizing ".." segments first, a guest path such as
// "/../../../data/data/some.other.app/files" would resolve to a host path
// outside the rootfs sandbox, allowing reads/writes of arbitrary
// app-private files. We normalize the path and re-verify the final host
// path still lives under g_rootfs before returning it.
// -------------------------------------------------------------------------
int path_guest_to_host(const char *guest_path, char *host_buf, size_t buf_size) {
    if (!guest_path || !host_buf || buf_size == 0) return -1;
    if (g_rootfs[0] == '\0') return -1;

    char normalized[1024];
    if (path_normalize(guest_path, normalized, sizeof(normalized)) != 0) return -1;

    // Strip leading slash for joining
    const char *rel = (normalized[0] == '/') ? normalized + 1 : normalized;
    int n = snprintf(host_buf, buf_size, "%s/%s", g_rootfs, rel);
    if (n <= 0 || (size_t)n >= buf_size) return -1;

    // Defense in depth: confirm the resolved path is still inside g_rootfs.
    size_t rootfs_len = strlen(g_rootfs);
    if (strncmp(host_buf, g_rootfs, rootfs_len) != 0) return -1;
    char after = host_buf[rootfs_len];
    if (after != '\0' && after != '/') return -1;

    return 0;
}

// -------------------------------------------------------------------------
// path_host_to_guest
// Converts a host path under rootfs to a guest-relative path.
// -------------------------------------------------------------------------
int path_host_to_guest(const char *host_path, char *guest_buf, size_t buf_size) {
    if (!host_path || !guest_buf || buf_size == 0) return -1;
    size_t rootfs_len = strlen(g_rootfs);

    if (strncmp(host_path, g_rootfs, rootfs_len) == 0) {
        const char *rel = host_path + rootfs_len;
        if (rel[0] == '\0') {
            strncpy(guest_buf, "/", buf_size);
        } else {
            strncpy(guest_buf, rel, buf_size - 1);
            guest_buf[buf_size - 1] = '\0';
        }
        return 0;
    }
    return -1;
}

// -------------------------------------------------------------------------
// path_expand_tilde
// Expands ~ to the home directory.
// -------------------------------------------------------------------------
int path_expand_tilde(const char *path, char *out_buf, size_t buf_size) {
    if (!path || !out_buf) return -1;

    if (path[0] == '~') {
        const char *home = g_home[0] ? g_home : "/root";
        int n = snprintf(out_buf, buf_size, "%s%s", home, path + 1);
        return (n > 0 && (size_t)n < buf_size) ? 0 : -1;
    }

    strncpy(out_buf, path, buf_size - 1);
    out_buf[buf_size - 1] = '\0';
    return 0;
}

// -------------------------------------------------------------------------
// path_normalize
// Collapses .. and . in a path. Simple implementation.
// -------------------------------------------------------------------------
int path_normalize(const char *path, char *out_buf, size_t buf_size) {
    if (!path || !out_buf || buf_size == 0) return -1;

    char tmp[1024];
    strncpy(tmp, path, sizeof(tmp) - 1);
    tmp[sizeof(tmp) - 1] = '\0';

    char *parts[128];
    int   part_count = 0;
    int   is_abs = (tmp[0] == '/');

    char *tok = strtok(tmp, "/");
    while (tok && part_count < 128) {
        if (strcmp(tok, ".") == 0) {
            // skip
        } else if (strcmp(tok, "..") == 0) {
            if (part_count > 0) part_count--;
        } else {
            parts[part_count++] = tok;
        }
        tok = strtok(NULL, "/");
    }

    size_t pos = 0;
    if (is_abs && pos < buf_size - 1) out_buf[pos++] = '/';

    for (int i = 0; i < part_count; i++) {
        if (i > 0 && pos < buf_size - 1) out_buf[pos++] = '/';
        size_t plen = strlen(parts[i]);
        if (pos + plen >= buf_size) break;
        memcpy(out_buf + pos, parts[i], plen);
        pos += plen;
    }

    if (pos == 0 && is_abs) out_buf[pos++] = '/';
    out_buf[pos] = '\0';
    return 0;
}

// -------------------------------------------------------------------------
// path_join
// -------------------------------------------------------------------------
int path_join(const char *base, const char *rel, char *out_buf, size_t buf_size) {
    if (!base || !rel || !out_buf) return -1;

    if (rel[0] == '/') {
        // Absolute — ignore base
        strncpy(out_buf, rel, buf_size - 1);
        out_buf[buf_size - 1] = '\0';
        return 0;
    }

    char tmp[1024];
    size_t base_len = strlen(base);
    if (base_len > 0 && base[base_len - 1] == '/') {
        snprintf(tmp, sizeof(tmp), "%s%s", base, rel);
    } else {
        snprintf(tmp, sizeof(tmp), "%s/%s", base, rel);
    }

    return path_normalize(tmp, out_buf, buf_size);
}

// -------------------------------------------------------------------------
// path_basename
// Returns pointer to last component of path.
// -------------------------------------------------------------------------
const char *path_basename(const char *path) {
    if (!path) return ".";
    const char *last = strrchr(path, '/');
    if (!last) return path;
    if (last[1] == '\0') return last; // trailing slash
    return last + 1;
}

// -------------------------------------------------------------------------
// path_dirname
// Writes the directory part of path into out_buf.
// -------------------------------------------------------------------------
int path_dirname(const char *path, char *out_buf, size_t buf_size) {
    if (!path || !out_buf || buf_size == 0) return -1;
    strncpy(out_buf, path, buf_size - 1);
    out_buf[buf_size - 1] = '\0';

    char *slash = strrchr(out_buf, '/');
    if (!slash) {
        strncpy(out_buf, ".", buf_size);
    } else if (slash == out_buf) {
        slash[1] = '\0';
    } else {
        *slash = '\0';
    }
    return 0;
}

// -------------------------------------------------------------------------
// path_sdcard_to_host
// Maps /sdcard paths to the real external storage path.
// -------------------------------------------------------------------------
int path_sdcard_to_host(const char *path, char *out_buf, size_t buf_size) {
    if (!path || !out_buf) return -1;
    if (strncmp(path, "/sdcard", 7) != 0) return -1;

    const char *sdcard = g_sdcard[0] ? g_sdcard : "/sdcard";
    const char *rest   = path + 7;
    int n = snprintf(out_buf, buf_size, "%s%s", sdcard, rest);
    return (n > 0 && (size_t)n < buf_size) ? 0 : -1;
}
