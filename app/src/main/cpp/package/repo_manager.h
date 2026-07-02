/*
 * VoidTerm - Repo Manager Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef REPO_MANAGER_H
#define REPO_MANAGER_H
#include <stddef.h>

typedef struct {
    char url[512];
    char dist[64];
    char components[256];
    int  enabled;
} repo_entry_t;

void               repo_manager_init(const char *proot_bin, const char *rootfs, const char *arch);
int                repo_add(const char *url, const char *dist, const char *components, int enabled);
int                repo_remove(int index);
int                repo_set_enabled(int index, int enabled);
int                repo_write_sources_list(void);
int                repo_fetch_inrelease(int repo_index, char *out_buf, size_t out_size);
int                repo_packages_url(int repo_index, const char *component, char *url_buf, size_t buf_size);
int                repo_get_count(void);
const repo_entry_t *repo_get(int index);
int                repo_check_connectivity(const char *mirror_url);
#endif
