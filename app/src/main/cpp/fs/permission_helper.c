/*
 * VoidTerm - Permission Helper
 * Checks and reports on file/directory permissions from the native layer.
 * The actual Android runtime permissions are requested in Java;
 * this layer checks the filesystem-level access we actually have.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "permission_helper.h"
#include <unistd.h>
#include <sys/stat.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-Perm"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// -------------------------------------------------------------------------
// perm_can_read
// -------------------------------------------------------------------------
int perm_can_read(const char *path) {
    return access(path, R_OK) == 0;
}

// -------------------------------------------------------------------------
// perm_can_write
// -------------------------------------------------------------------------
int perm_can_write(const char *path) {
    return access(path, W_OK) == 0;
}

// -------------------------------------------------------------------------
// perm_can_execute
// -------------------------------------------------------------------------
int perm_can_execute(const char *path) {
    return access(path, X_OK) == 0;
}

// -------------------------------------------------------------------------
// perm_get_mode_string
// Returns a string like "rwxr-xr-x" for the path.
// -------------------------------------------------------------------------
int perm_get_mode_string(const char *path, char out[11]) {
    struct stat st;
    if (stat(path, &st) != 0) {
        strncpy(out, "----------", 11);
        return -1;
    }

    mode_t m = st.st_mode;
    out[0]  = S_ISDIR(m)  ? 'd' : (S_ISLNK(m) ? 'l' : '-');
    out[1]  = (m & S_IRUSR) ? 'r' : '-';
    out[2]  = (m & S_IWUSR) ? 'w' : '-';
    out[3]  = (m & S_IXUSR) ? 'x' : '-';
    out[4]  = (m & S_IRGRP) ? 'r' : '-';
    out[5]  = (m & S_IWGRP) ? 'w' : '-';
    out[6]  = (m & S_IXGRP) ? 'x' : '-';
    out[7]  = (m & S_IROTH) ? 'r' : '-';
    out[8]  = (m & S_IWOTH) ? 'w' : '-';
    out[9]  = (m & S_IXOTH) ? 'x' : '-';
    out[10] = '\0';
    return 0;
}

// -------------------------------------------------------------------------
// perm_make_executable
// -------------------------------------------------------------------------
int perm_make_executable(const char *path) {
    struct stat st;
    if (stat(path, &st) != 0) return -1;
    // Only grant execute to owner and group — avoid making files
    // world-executable, which would let any other app/process on a
    // multi-user or shared filesystem execute them.
    return chmod(path, st.st_mode | S_IXUSR | S_IXGRP);
}

// -------------------------------------------------------------------------
// perm_check_sdcard
// Returns 1 if /sdcard is readable, 0 otherwise.
// -------------------------------------------------------------------------
int perm_check_sdcard(void) {
    return access("/sdcard", R_OK) == 0;
}

// -------------------------------------------------------------------------
// perm_check_sdcard_write
// Returns 1 if /sdcard is writable, 0 otherwise.
// -------------------------------------------------------------------------
int perm_check_sdcard_write(void) {
    return access("/sdcard", W_OK) == 0;
}

// -------------------------------------------------------------------------
// perm_is_root
// -------------------------------------------------------------------------
int perm_is_root(void) {
    return getuid() == 0;
}

// -------------------------------------------------------------------------
// perm_get_uid
// -------------------------------------------------------------------------
int perm_get_uid(void) {
    return (int)getuid();
}
