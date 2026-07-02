/*
 * VoidTerm - Repository Manager
 * Manages Kali Linux APT repository sources, mirrors, and keyring.
 * Handles InRelease file fetching, parsing, and verification.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "repo_manager.h"
#include "../net/http_client.h"
#include "../crypto/sha256.h"
#include "../fs/fs_utils.h"
#include "../terminal/process_runner.h"
#include "../shell/shell_quote.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-Repo"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define MAX_REPOS 16
#define INRELEASE_BUF_SIZE (256 * 1024)

static repo_entry_t g_repos[MAX_REPOS];
static int          g_repo_count = 0;

static char g_rootfs[512]    = {0};
static char g_proot_bin[512] = {0};
static char g_arch[32]       = "arm64";

// -------------------------------------------------------------------------
// repo_manager_init
// -------------------------------------------------------------------------
void repo_manager_init(const char *proot_bin, const char *rootfs, const char *arch) {
    if (proot_bin) strncpy(g_proot_bin, proot_bin, sizeof(g_proot_bin) - 1);
    if (rootfs)    strncpy(g_rootfs,    rootfs,    sizeof(g_rootfs)    - 1);
    if (arch)      strncpy(g_arch,      arch,      sizeof(g_arch)      - 1);
    g_repo_count = 0;

    // Add default Kali repo
    repo_add("https://http.kali.org/kali", "kali-rolling",
              "main contrib non-free non-free-firmware", 1);
}

// -------------------------------------------------------------------------
// repo_add
// -------------------------------------------------------------------------
int repo_add(const char *url, const char *dist,
              const char *components, int enabled) {
    if (g_repo_count >= MAX_REPOS) return -1;
    repo_entry_t *r = &g_repos[g_repo_count];
    memset(r, 0, sizeof(*r));
    strncpy(r->url,        url,        sizeof(r->url)        - 1);
    strncpy(r->dist,       dist,       sizeof(r->dist)       - 1);
    strncpy(r->components, components, sizeof(r->components) - 1);
    r->enabled = enabled;
    g_repo_count++;
    LOGI("Repo added: %s %s %s", url, dist, components);
    return g_repo_count - 1;
}

// -------------------------------------------------------------------------
// repo_remove
// -------------------------------------------------------------------------
int repo_remove(int index) {
    if (index < 0 || index >= g_repo_count) return -1;
    memmove(&g_repos[index], &g_repos[index + 1],
            (size_t)(g_repo_count - index - 1) * sizeof(repo_entry_t));
    g_repo_count--;
    return 0;
}

// -------------------------------------------------------------------------
// repo_set_enabled
// -------------------------------------------------------------------------
int repo_set_enabled(int index, int enabled) {
    if (index < 0 || index >= g_repo_count) return -1;
    g_repos[index].enabled = enabled;
    return 0;
}

// -------------------------------------------------------------------------
// repo_write_sources_list
// Writes all enabled repos to /etc/apt/sources.list in the rootfs.
// -------------------------------------------------------------------------
int repo_write_sources_list(void) {
    char path[512];
    snprintf(path, sizeof(path), "%s/etc/apt/sources.list", g_rootfs);

    FILE *f = fopen(path, "w");
    if (!f) {
        LOGE("Cannot open sources.list: %s", path);
        return -1;
    }

    fprintf(f, "# VoidTerm - Automatically generated sources.list\n");
    fprintf(f, "# Developer: Asotn | github.com/Asotn\n\n");

    for (int i = 0; i < g_repo_count; i++) {
        if (!g_repos[i].enabled) {
            fprintf(f, "# deb %s %s %s\n",
                    g_repos[i].url, g_repos[i].dist, g_repos[i].components);
        } else {
            fprintf(f, "deb %s %s %s\n",
                    g_repos[i].url, g_repos[i].dist, g_repos[i].components);
        }
    }

    fclose(f);
    LOGI("sources.list written: %d repos", g_repo_count);
    return 0;
}

// -------------------------------------------------------------------------
// repo_fetch_inrelease
// Downloads the InRelease file for a repo. NOTE: this function only
// fetches the file — it does not itself perform any cryptographic
// verification. Trust is established by apt-get's own gpg signature
// checking (against kali-archive-keyring) when apt_update() runs the
// real apt-get inside the proot environment. Callers must not treat a
// successful fetch here as a verified/trusted release file.
// -------------------------------------------------------------------------
int repo_fetch_inrelease(int repo_index, char *out_buf, size_t out_size) {
    if (repo_index < 0 || repo_index >= g_repo_count) return -1;
    repo_entry_t *r = &g_repos[repo_index];

    char url[1024];
    snprintf(url, sizeof(url), "%s/dists/%s/InRelease", r->url, r->dist);

    LOGI("Fetching: %s", url);
    int ret = http_fetch_string(url, out_buf, out_size);
    if (ret != 0) {
        LOGE("Failed to fetch InRelease from %s", url);
        return -1;
    }

    return 0;
}

// -------------------------------------------------------------------------
// repo_parse_packages_url
// Builds the Packages.gz URL for a given repo and architecture.
// -------------------------------------------------------------------------
int repo_packages_url(int repo_index, const char *component,
                       char *url_buf, size_t buf_size) {
    if (repo_index < 0 || repo_index >= g_repo_count) return -1;
    repo_entry_t *r = &g_repos[repo_index];

    int n = snprintf(url_buf, buf_size,
        "%s/dists/%s/%s/binary-%s/Packages.gz",
        r->url, r->dist, component, g_arch);

    return (n > 0 && (size_t)n < buf_size) ? 0 : -1;
}

// -------------------------------------------------------------------------
// repo_get_count
// -------------------------------------------------------------------------
int repo_get_count(void) {
    return g_repo_count;
}

// -------------------------------------------------------------------------
// repo_get
// -------------------------------------------------------------------------
const repo_entry_t *repo_get(int index) {
    if (index < 0 || index >= g_repo_count) return NULL;
    return &g_repos[index];
}

// -------------------------------------------------------------------------
// repo_check_connectivity
// Checks if the Kali mirror is reachable.
// Returns 0 if reachable, -1 otherwise.
// -------------------------------------------------------------------------
int repo_check_connectivity(const char *mirror_url) {
    if (!mirror_url) return -1;
    char out[256] = {0};
    char url_expr[1300], q_url[1400], cmd[1600];
    snprintf(url_expr, sizeof(url_expr), "%s/dists/kali-rolling/Release", mirror_url);
    if (shell_quote(url_expr, q_url, sizeof(q_url)) != 0) {
        LOGE("repo_check_connectivity: URL too long/unsafe to quote");
        return -1;
    }
    snprintf(cmd, sizeof(cmd),
        "curl -sL --proto '=https' --connect-timeout 10 --max-time 15 -o /dev/null -w '%%{http_code}' %s",
        q_url);

    int ret = process_run_proot(g_proot_bin, g_rootfs, cmd, out, sizeof(out));
    if (ret != 0) return -1;

    int code = atoi(out);
    LOGI("Mirror check %s: HTTP %d", mirror_url, code);
    return (code >= 200 && code < 400) ? 0 : -1;
}
