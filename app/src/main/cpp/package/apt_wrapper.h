/*
 * VoidTerm - APT Wrapper Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef APT_WRAPPER_H
#define APT_WRAPPER_H
#include <stddef.h>

void        apt_init(const char *proot_bin, const char *rootfs, const char *mirror);
int         apt_update(char *out, size_t out_size);
int         apt_upgrade(int dist_upgrade, char *out, size_t out_size);
int         apt_install(const char *pkg_list, char *out, size_t out_size);
int         apt_remove(const char *pkg_list, int purge, char *out, size_t out_size);
int         apt_autoremove(char *out, size_t out_size);
int         apt_autoclean(char *out, size_t out_size);
int         apt_fix_broken(char *out, size_t out_size);
int         apt_search(const char *query, char *out, size_t out_size);
int         apt_show(const char *pkg_name, char *out, size_t out_size);
int         apt_list_upgradable(char *out, size_t out_size);
int         apt_download_only(const char *pkg_name, char *out, size_t out_size);
long        apt_get_cache_size(void);
int         apt_write_sources_list(const char *mirror, const char *dist, const char *components);
void        apt_set_mirror(const char *mirror);
const char *apt_get_mirror(void);
int         apt_parse_progress_line(const char *line);
#endif
