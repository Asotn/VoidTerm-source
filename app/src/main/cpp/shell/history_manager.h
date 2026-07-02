/*
 * VoidTerm - History Manager Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef HISTORY_MANAGER_H
#define HISTORY_MANAGER_H

#define HISTORY_MAX_PATH 256

typedef struct {
    char  **entries;
    int     max_entries;
    int     count;
    int     position;
    char    file_path[HISTORY_MAX_PATH];
} history_t;

int         history_init(history_t *h, int max_entries, const char *history_file);
void        history_destroy(history_t *h);
int         history_add(history_t *h, const char *cmd);
const char *history_prev(history_t *h);
const char *history_next(history_t *h);
void        history_reset_nav(history_t *h);
const char *history_get(const history_t *h, int index);
int         history_save(const history_t *h);
int         history_load(history_t *h);
const char *history_search(const history_t *h, const char *prefix);
void        history_clear(history_t *h);

#endif /* HISTORY_MANAGER_H */
