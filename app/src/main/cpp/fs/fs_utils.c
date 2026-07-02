/*
 * VoidTerm - Filesystem Utilities
 * Helper functions for file/directory operations used by the app and package engine.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "fs_utils.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-FS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// -------------------------------------------------------------------------
// fs_exists
// -------------------------------------------------------------------------
int fs_exists(const char *path) {
    return access(path, F_OK) == 0;
}

// -------------------------------------------------------------------------
// fs_is_dir
// -------------------------------------------------------------------------
int fs_is_dir(const char *path) {
    struct stat st;
    if (stat(path, &st) != 0) return 0;
    return S_ISDIR(st.st_mode);
}

// -------------------------------------------------------------------------
// fs_is_file
// -------------------------------------------------------------------------
int fs_is_file(const char *path) {
    struct stat st;
    if (stat(path, &st) != 0) return 0;
    return S_ISREG(st.st_mode);
}

// -------------------------------------------------------------------------
// fs_mkdirs
// Creates directory and all parents (like mkdir -p).
// -------------------------------------------------------------------------
int fs_mkdirs(const char *path, mode_t mode) {
    char tmp[1024];
    strncpy(tmp, path, sizeof(tmp) - 1);
    size_t len = strlen(tmp);
    if (len == 0) return -1;
    if (tmp[len-1] == '/') tmp[len-1] = '\0';

    for (char *p = tmp + 1; *p; p++) {
        if (*p == '/') {
            *p = '\0';
            if (mkdir(tmp, mode) != 0 && errno != EEXIST) return -1;
            *p = '/';
        }
    }
    if (mkdir(tmp, mode) != 0 && errno != EEXIST) return -1;
    return 0;
}

// -------------------------------------------------------------------------
// fs_file_size
// -------------------------------------------------------------------------
long fs_file_size(const char *path) {
    struct stat st;
    if (stat(path, &st) != 0) return -1;
    return (long)st.st_size;
}

// -------------------------------------------------------------------------
// fs_copy_file
// -------------------------------------------------------------------------
int fs_copy_file(const char *src, const char *dst) {
    FILE *in  = fopen(src, "rb");
    FILE *out = fopen(dst, "wb");
    if (!in || !out) {
        if (in)  fclose(in);
        if (out) fclose(out);
        return -1;
    }

    char buf[8192];
    size_t n;
    while ((n = fread(buf, 1, sizeof(buf), in)) > 0) {
        fwrite(buf, 1, n, out);
    }

    fclose(in);
    fclose(out);
    return 0;
}

// -------------------------------------------------------------------------
// fs_delete_file
// -------------------------------------------------------------------------
int fs_delete_file(const char *path) {
    return unlink(path) == 0 ? 0 : -1;
}

// -------------------------------------------------------------------------
// fs_delete_dir_recursive
// -------------------------------------------------------------------------
int fs_delete_dir_recursive(const char *path) {
    DIR *d = opendir(path);
    if (!d) return unlink(path);

    struct dirent *entry;
    while ((entry = readdir(d))) {
        if (strcmp(entry->d_name, ".") == 0 ||
            strcmp(entry->d_name, "..") == 0) continue;

        char child[1024];
        snprintf(child, sizeof(child), "%s/%s", path, entry->d_name);
        if (entry->d_type == DT_DIR) {
            fs_delete_dir_recursive(child);
        } else {
            unlink(child);
        }
    }
    closedir(d);
    return rmdir(path);
}

// -------------------------------------------------------------------------
// fs_read_file
// Reads entire file into a malloc'd buffer. Caller must free.
// Returns NULL on failure.
// -------------------------------------------------------------------------
char *fs_read_file(const char *path, size_t *out_len) {
    FILE *f = fopen(path, "rb");
    if (!f) return NULL;

    fseek(f, 0, SEEK_END);
    long size = ftell(f);
    fseek(f, 0, SEEK_SET);

    if (size < 0) { fclose(f); return NULL; }

    char *buf = malloc((size_t)size + 1);
    if (!buf) { fclose(f); return NULL; }

    fread(buf, 1, (size_t)size, f);
    buf[size] = '\0';
    fclose(f);

    if (out_len) *out_len = (size_t)size;
    return buf;
}

// -------------------------------------------------------------------------
// fs_write_file
// -------------------------------------------------------------------------
int fs_write_file(const char *path, const char *data, size_t len) {
    FILE *f = fopen(path, "wb");
    if (!f) return -1;
    fwrite(data, 1, len, f);
    fclose(f);
    return 0;
}

// -------------------------------------------------------------------------
// fs_append_file
// -------------------------------------------------------------------------
int fs_append_file(const char *path, const char *data, size_t len) {
    FILE *f = fopen(path, "ab");
    if (!f) return -1;
    fwrite(data, 1, len, f);
    fclose(f);
    return 0;
}

// -------------------------------------------------------------------------
// fs_chmod
// -------------------------------------------------------------------------
int fs_chmod(const char *path, mode_t mode) {
    return chmod(path, mode) == 0 ? 0 : -1;
}

// -------------------------------------------------------------------------
// fs_list_dir
// Calls callback for each entry. Returns entry count.
// -------------------------------------------------------------------------
int fs_list_dir(const char *path, fs_dir_entry_fn callback, void *userdata) {
    DIR *d = opendir(path);
    if (!d) return -1;

    int count = 0;
    struct dirent *entry;
    while ((entry = readdir(d))) {
        if (strcmp(entry->d_name, ".") == 0 ||
            strcmp(entry->d_name, "..") == 0) continue;
        if (callback) callback(entry->d_name, entry->d_type, userdata);
        count++;
    }
    closedir(d);
    return count;
}

// -------------------------------------------------------------------------
// fs_get_free_space
// Returns free bytes on the filesystem containing path.
// -------------------------------------------------------------------------
long fs_get_free_space(const char *path) {
    struct statvfs sv;
    if (statvfs(path, &sv) != 0) return -1;
    return (long)(sv.f_bavail * sv.f_frsize);
}
