/*
 * VoidTerm - Path Resolver Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef PATH_RESOLVER_H
#define PATH_RESOLVER_H
#include <stddef.h>
void        path_resolver_init(const char *rootfs, const char *home, const char *sdcard);
int         path_guest_to_host(const char *guest_path, char *host_buf, size_t buf_size);
int         path_host_to_guest(const char *host_path, char *guest_buf, size_t buf_size);
int         path_expand_tilde(const char *path, char *out_buf, size_t buf_size);
int         path_normalize(const char *path, char *out_buf, size_t buf_size);
int         path_join(const char *base, const char *rel, char *out_buf, size_t buf_size);
const char *path_basename(const char *path);
int         path_dirname(const char *path, char *out_buf, size_t buf_size);
int         path_sdcard_to_host(const char *path, char *out_buf, size_t buf_size);
#endif
