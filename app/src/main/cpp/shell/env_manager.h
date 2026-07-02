/*
 * VoidTerm - Env Manager Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef ENV_MANAGER_H
#define ENV_MANAGER_H

void        env_init(void);
int         env_set(const char *key, const char *value);
const char *env_get(const char *key);
int         env_unset(const char *key);
char      **env_build_array(void);
void        env_free_array(char **arr);
int         env_count_entries(void);
void        env_destroy(void);

#endif /* ENV_MANAGER_H */
