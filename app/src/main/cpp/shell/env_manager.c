/*
 * VoidTerm - Environment Manager (C layer)
 * Manages environment variable key-value store.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "env_manager.h"
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-Env"
#define MAX_ENV_ENTRIES 256

typedef struct {
    char *key;
    char *value;
} env_entry_t;

static env_entry_t env_table[MAX_ENV_ENTRIES];
static int         env_count = 0;

// -------------------------------------------------------------------------
// env_init
// -------------------------------------------------------------------------
void env_init(void) {
    memset(env_table, 0, sizeof(env_table));
    env_count = 0;
}

// -------------------------------------------------------------------------
// env_set
// -------------------------------------------------------------------------
int env_set(const char *key, const char *value) {
    if (!key || !value) return -1;

    // Update existing
    for (int i = 0; i < env_count; i++) {
        if (env_table[i].key && strcmp(env_table[i].key, key) == 0) {
            free(env_table[i].value);
            env_table[i].value = strdup(value);
            return env_table[i].value ? 0 : -1;
        }
    }

    // Add new
    if (env_count >= MAX_ENV_ENTRIES) return -1;
    env_table[env_count].key   = strdup(key);
    env_table[env_count].value = strdup(value);
    if (!env_table[env_count].key || !env_table[env_count].value) return -1;
    env_count++;
    return 0;
}

// -------------------------------------------------------------------------
// env_get
// -------------------------------------------------------------------------
const char *env_get(const char *key) {
    if (!key) return NULL;
    for (int i = 0; i < env_count; i++) {
        if (env_table[i].key && strcmp(env_table[i].key, key) == 0) {
            return env_table[i].value;
        }
    }
    return NULL;
}

// -------------------------------------------------------------------------
// env_unset
// -------------------------------------------------------------------------
int env_unset(const char *key) {
    if (!key) return -1;
    for (int i = 0; i < env_count; i++) {
        if (env_table[i].key && strcmp(env_table[i].key, key) == 0) {
            free(env_table[i].key);
            free(env_table[i].value);
            // Shift
            memmove(&env_table[i], &env_table[i+1],
                    (size_t)(env_count - i - 1) * sizeof(env_entry_t));
            env_count--;
            return 0;
        }
    }
    return -1;
}

// -------------------------------------------------------------------------
// env_build_array
// Builds a NULL-terminated envp array. Caller must free the array.
// -------------------------------------------------------------------------
char **env_build_array(void) {
    char **arr = malloc((size_t)(env_count + 1) * sizeof(char *));
    if (!arr) return NULL;
    for (int i = 0; i < env_count; i++) {
        size_t len = strlen(env_table[i].key) + 1 + strlen(env_table[i].value) + 1;
        arr[i] = malloc(len);
        if (arr[i]) {
            snprintf(arr[i], len, "%s=%s", env_table[i].key, env_table[i].value);
        }
    }
    arr[env_count] = NULL;
    return arr;
}

// -------------------------------------------------------------------------
// env_free_array
// -------------------------------------------------------------------------
void env_free_array(char **arr) {
    if (!arr) return;
    for (int i = 0; arr[i]; i++) free(arr[i]);
    free(arr);
}

// -------------------------------------------------------------------------
// env_count_entries
// -------------------------------------------------------------------------
int env_count_entries(void) {
    return env_count;
}

// -------------------------------------------------------------------------
// env_destroy
// -------------------------------------------------------------------------
void env_destroy(void) {
    for (int i = 0; i < env_count; i++) {
        free(env_table[i].key);
        free(env_table[i].value);
    }
    env_count = 0;
}
