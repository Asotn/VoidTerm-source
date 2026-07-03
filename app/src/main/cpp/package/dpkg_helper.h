/*
 * VoidTerm - dpkg Helper Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef DPKG_HELPER_H
#define DPKG_HELPER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stddef.h>

typedef enum {
    DPKG_STATUS_UNKNOWN = 0,
    DPKG_STATUS_INSTALLED,
    DPKG_STATUS_NOT_INSTALLED,
    DPKG_STATUS_CONFIG_FILES,
    DPKG_STATUS_UNPACKED,
    DPKG_STATUS_HALF_CONFIGURED,
    DPKG_STATUS_HALF_INSTALLED
} dpkg_status_t;

void        dpkg_init(const char *proot_bin, const char *rootfs);
int         dpkg_is_installed(const char *pkg_name);
int         dpkg_get_version(const char *pkg_name, char *version_buf, size_t buf_size);
int         dpkg_get_status(const char *pkg_name, dpkg_status_t *status);
int         dpkg_list_installed(char *buf, size_t buf_size);
int         dpkg_get_installed_count(void);
int         dpkg_get_files(const char *pkg_name, char *buf, size_t buf_size);
int         dpkg_reconfigure(const char *pkg_name, char *out, size_t out_size);
int         dpkg_fix_broken(char *out, size_t out_size);
const char *dpkg_status_to_string(dpkg_status_t status);

#ifdef __cplusplus
}
#endif

#endif
