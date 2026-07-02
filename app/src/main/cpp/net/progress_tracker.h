/*
 * VoidTerm - Progress Tracker Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef PROGRESS_TRACKER_H
#define PROGRESS_TRACKER_H
typedef void (*progress_callback_t)(long downloaded, long total, int pct, void *userdata);
void progress_tracker_init(progress_callback_t cb, void *userdata);
int  progress_tracker_add(const char *url, long total_size);
void progress_tracker_update(int id, long downloaded);
void progress_tracker_complete(int id);
void progress_tracker_reset(void);
int  progress_tracker_all_done(void);
#endif
