/*
 * VoidTerm - History Manager
 * Persists and navigates command history (like .bash_history).
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "history_manager.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-Hist"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// -------------------------------------------------------------------------
// history_init
// -------------------------------------------------------------------------
int history_init(history_t *h, int max_entries, const char *history_file) {
    if (!h || max_entries <= 0) return -1;
    memset(h, 0, sizeof(*h));

    h->entries = calloc((size_t)max_entries, sizeof(char *));
    if (!h->entries) return -1;

    h->max_entries = max_entries;
    h->count       = 0;
    h->position    = -1;

    if (history_file && strlen(history_file) < sizeof(h->file_path) - 1) {
        strncpy(h->file_path, history_file, sizeof(h->file_path) - 1);
        history_load(h);
    }

    LOGI("History initialized: max=%d, file=%s", max_entries,
         history_file ? history_file : "(none)");
    return 0;
}

// -------------------------------------------------------------------------
// history_destroy
// -------------------------------------------------------------------------
void history_destroy(history_t *h) {
    if (!h) return;
    history_save(h);
    for (int i = 0; i < h->count; i++) {
        free(h->entries[i]);
    }
    free(h->entries);
    memset(h, 0, sizeof(*h));
}

// -------------------------------------------------------------------------
// history_add
// -------------------------------------------------------------------------
int history_add(history_t *h, const char *cmd) {
    if (!h || !cmd || strlen(cmd) == 0) return -1;

    // Skip duplicates of the last entry
    if (h->count > 0 && strcmp(h->entries[h->count - 1], cmd) == 0) {
        h->position = -1;
        return 0;
    }

    if (h->count >= h->max_entries) {
        // Evict oldest
        free(h->entries[0]);
        memmove(h->entries, h->entries + 1,
                (size_t)(h->max_entries - 1) * sizeof(char *));
        h->count = h->max_entries - 1;
    }

    h->entries[h->count] = strdup(cmd);
    if (!h->entries[h->count]) return -1;
    h->count++;
    h->position = -1; // reset navigation
    return 0;
}

// -------------------------------------------------------------------------
// history_prev
// Navigate backwards (up arrow).
// -------------------------------------------------------------------------
const char *history_prev(history_t *h) {
    if (!h || h->count == 0) return NULL;
    if (h->position < 0) h->position = h->count;
    if (h->position > 0) h->position--;
    return h->entries[h->position];
}

// -------------------------------------------------------------------------
// history_next
// Navigate forwards (down arrow).
// -------------------------------------------------------------------------
const char *history_next(history_t *h) {
    if (!h || h->position < 0) return NULL;
    h->position++;
    if (h->position >= h->count) {
        h->position = -1;
        return "";
    }
    return h->entries[h->position];
}

// -------------------------------------------------------------------------
// history_reset_nav
// -------------------------------------------------------------------------
void history_reset_nav(history_t *h) {
    if (h) h->position = -1;
}

// -------------------------------------------------------------------------
// history_get
// Get entry by index (0 = oldest).
// -------------------------------------------------------------------------
const char *history_get(const history_t *h, int index) {
    if (!h || index < 0 || index >= h->count) return NULL;
    return h->entries[index];
}

// -------------------------------------------------------------------------
// history_save
// -------------------------------------------------------------------------
int history_save(const history_t *h) {
    if (!h || h->file_path[0] == '\0') return -1;
    FILE *f = fopen(h->file_path, "w");
    if (!f) return -1;
    for (int i = 0; i < h->count; i++) {
        fprintf(f, "%s\n", h->entries[i]);
    }
    fclose(f);
    LOGI("History saved: %d entries to %s", h->count, h->file_path);
    return 0;
}

// -------------------------------------------------------------------------
// history_load
// -------------------------------------------------------------------------
int history_load(history_t *h) {
    if (!h || h->file_path[0] == '\0') return -1;
    FILE *f = fopen(h->file_path, "r");
    if (!f) return -1;

    char line[4096];
    while (fgets(line, sizeof(line), f)) {
        // Strip trailing newline
        size_t len = strlen(line);
        while (len > 0 && (line[len-1] == '\n' || line[len-1] == '\r')) {
            line[--len] = '\0';
        }
        if (len > 0) history_add(h, line);
    }
    fclose(f);
    LOGI("History loaded: %d entries from %s", h->count, h->file_path);
    return 0;
}

// -------------------------------------------------------------------------
// history_search
// Returns the most recent entry matching prefix.
// -------------------------------------------------------------------------
const char *history_search(const history_t *h, const char *prefix) {
    if (!h || !prefix) return NULL;
    size_t plen = strlen(prefix);
    for (int i = h->count - 1; i >= 0; i--) {
        if (strncmp(h->entries[i], prefix, plen) == 0) {
            return h->entries[i];
        }
    }
    return NULL;
}

// -------------------------------------------------------------------------
// history_clear
// -------------------------------------------------------------------------
void history_clear(history_t *h) {
    if (!h) return;
    for (int i = 0; i < h->count; i++) {
        free(h->entries[i]);
        h->entries[i] = NULL;
    }
    h->count    = 0;
    h->position = -1;
}
