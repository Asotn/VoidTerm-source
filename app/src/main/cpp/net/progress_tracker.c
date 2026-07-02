/*
 * VoidTerm - Progress Tracker
 * Tracks multi-file download progress, calculates overall percentage.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "progress_tracker.h"
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-Progress"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define MAX_TRACKED_FILES 64

typedef struct {
    char  url[512];
    long  downloaded;
    long  total;
    int   done;
} tracked_file_t;

static tracked_file_t tracked[MAX_TRACKED_FILES];
static int            tracked_count = 0;
static progress_callback_t g_callback = NULL;
static void              *g_userdata  = NULL;

// -------------------------------------------------------------------------
// progress_tracker_init
// -------------------------------------------------------------------------
void progress_tracker_init(progress_callback_t cb, void *userdata) {
    memset(tracked, 0, sizeof(tracked));
    tracked_count = 0;
    g_callback    = cb;
    g_userdata    = userdata;
}

// -------------------------------------------------------------------------
// progress_tracker_add
// -------------------------------------------------------------------------
int progress_tracker_add(const char *url, long total_size) {
    if (tracked_count >= MAX_TRACKED_FILES) return -1;
    tracked_file_t *f = &tracked[tracked_count];
    strncpy(f->url, url, sizeof(f->url) - 1);
    f->downloaded = 0;
    f->total      = total_size;
    f->done       = 0;
    return tracked_count++;
}

// -------------------------------------------------------------------------
// progress_tracker_update
// -------------------------------------------------------------------------
void progress_tracker_update(int id, long downloaded) {
    if (id < 0 || id >= tracked_count) return;
    tracked[id].downloaded = downloaded;
    if (tracked[id].total > 0 && downloaded >= tracked[id].total) {
        tracked[id].done = 1;
    }

    // Calculate aggregate progress
    long total_dl    = 0;
    long total_bytes = 0;
    for (int i = 0; i < tracked_count; i++) {
        total_dl    += tracked[i].downloaded;
        total_bytes += tracked[i].total > 0 ? tracked[i].total : 0;
    }

    int overall_pct = total_bytes > 0
        ? (int)((total_dl * 100) / total_bytes)
        : -1;

    if (g_callback) {
        g_callback(total_dl, total_bytes, overall_pct, g_userdata);
    }
}

// -------------------------------------------------------------------------
// progress_tracker_complete
// -------------------------------------------------------------------------
void progress_tracker_complete(int id) {
    if (id < 0 || id >= tracked_count) return;
    tracked[id].done = 1;
    if (tracked[id].total > 0) {
        tracked[id].downloaded = tracked[id].total;
    }
    progress_tracker_update(id, tracked[id].downloaded);
}

// -------------------------------------------------------------------------
// progress_tracker_reset
// -------------------------------------------------------------------------
void progress_tracker_reset(void) {
    memset(tracked, 0, sizeof(tracked));
    tracked_count = 0;
}

// -------------------------------------------------------------------------
// progress_tracker_all_done
// -------------------------------------------------------------------------
int progress_tracker_all_done(void) {
    for (int i = 0; i < tracked_count; i++) {
        if (!tracked[i].done) return 0;
    }
    return 1;
}
