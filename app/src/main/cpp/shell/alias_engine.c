/*
 * VoidTerm - Alias Engine
 * Stores and expands shell aliases (e.g., alias ll='ls -la').
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "alias_engine.h"
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

#define LOG_TAG  "VoidTerm-Alias"
#define MAX_ALIASES 128

typedef struct {
    char *name;
    char *expansion;
} alias_entry_t;

static alias_entry_t alias_table[MAX_ALIASES];
static int           alias_count = 0;

// -------------------------------------------------------------------------
// alias_init
// -------------------------------------------------------------------------
void alias_init(void) {
    memset(alias_table, 0, sizeof(alias_table));
    alias_count = 0;

    // Default aliases
    alias_set("ll",     "ls -alF");
    alias_set("la",     "ls -A");
    alias_set("l",      "ls -CF");
    alias_set("...",    "cd ../.."); 
    alias_set("....",   "cd ../../..");
    alias_set("cls",    "clear");
    alias_set("apt",    "apt-get");
    alias_set("update", "apt-get update");
    alias_set("install","apt-get install -y");
    alias_set("search", "apt-cache search");
    alias_set("remove", "apt-get remove -y");
    alias_set("purge",  "apt-get autoremove --purge -y");
    alias_set("upgrade","apt-get upgrade -y");
    alias_set("dist-upgrade", "apt-get dist-upgrade -y");
}

// -------------------------------------------------------------------------
// alias_set
// -------------------------------------------------------------------------
int alias_set(const char *name, const char *expansion) {
    if (!name || !expansion) return -1;

    // Update existing
    for (int i = 0; i < alias_count; i++) {
        if (alias_table[i].name && strcmp(alias_table[i].name, name) == 0) {
            free(alias_table[i].expansion);
            alias_table[i].expansion = strdup(expansion);
            return alias_table[i].expansion ? 0 : -1;
        }
    }

    if (alias_count >= MAX_ALIASES) return -1;
    alias_table[alias_count].name      = strdup(name);
    alias_table[alias_count].expansion = strdup(expansion);
    if (!alias_table[alias_count].name || !alias_table[alias_count].expansion)
        return -1;
    alias_count++;
    return 0;
}

// -------------------------------------------------------------------------
// alias_get
// -------------------------------------------------------------------------
const char *alias_get(const char *name) {
    if (!name) return NULL;
    for (int i = 0; i < alias_count; i++) {
        if (alias_table[i].name && strcmp(alias_table[i].name, name) == 0) {
            return alias_table[i].expansion;
        }
    }
    return NULL;
}

// -------------------------------------------------------------------------
// alias_unset
// -------------------------------------------------------------------------
int alias_unset(const char *name) {
    if (!name) return -1;
    for (int i = 0; i < alias_count; i++) {
        if (alias_table[i].name && strcmp(alias_table[i].name, name) == 0) {
            free(alias_table[i].name);
            free(alias_table[i].expansion);
            memmove(&alias_table[i], &alias_table[i+1],
                    (size_t)(alias_count - i - 1) * sizeof(alias_entry_t));
            alias_count--;
            return 0;
        }
    }
    return -1;
}

// -------------------------------------------------------------------------
// alias_expand
// Expands the first word in cmd if it matches an alias.
// Writes result into out_buf. Returns 1 if expanded, 0 if not.
// -------------------------------------------------------------------------
int alias_expand(const char *cmd, char *out_buf, size_t out_buf_size) {
    if (!cmd || !out_buf || out_buf_size == 0) return 0;

    // Find the first word
    const char *p = cmd;
    while (*p == ' ') p++;
    const char *word_start = p;
    while (*p && *p != ' ') p++;
    size_t word_len = (size_t)(p - word_start);

    if (word_len == 0) return 0;

    // Look up alias
    char word[256];
    if (word_len >= sizeof(word)) return 0;
    memcpy(word, word_start, word_len);
    word[word_len] = '\0';

    const char *expansion = alias_get(word);
    if (!expansion) return 0;

    // Build expanded command
    const char *rest = p;
    while (*rest == ' ') rest++;

    if (*rest) {
        snprintf(out_buf, out_buf_size, "%s %s", expansion, rest);
    } else {
        snprintf(out_buf, out_buf_size, "%s", expansion);
    }
    return 1;
}

// -------------------------------------------------------------------------
// alias_list
// Write a list of all aliases into buf.
// -------------------------------------------------------------------------
int alias_list(char *buf, size_t buf_size) {
    if (!buf || buf_size == 0) return 0;
    size_t pos = 0;
    for (int i = 0; i < alias_count; i++) {
        int n = snprintf(buf + pos, buf_size - pos,
                         "alias %s='%s'\n",
                         alias_table[i].name, alias_table[i].expansion);
        if (n < 0 || (size_t)n >= buf_size - pos) break;
        pos += (size_t)n;
    }
    return (int)pos;
}

// -------------------------------------------------------------------------
// alias_destroy
// -------------------------------------------------------------------------
void alias_destroy(void) {
    for (int i = 0; i < alias_count; i++) {
        free(alias_table[i].name);
        free(alias_table[i].expansion);
    }
    alias_count = 0;
}
