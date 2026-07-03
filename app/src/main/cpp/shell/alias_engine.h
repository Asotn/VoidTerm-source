/*
 * VoidTerm - Alias Engine Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef ALIAS_ENGINE_H
#define ALIAS_ENGINE_H

#ifdef __cplusplus
extern "C" {
#endif


#include <stddef.h>

void        alias_init(void);
int         alias_set(const char *name, const char *expansion);
const char *alias_get(const char *name);
int         alias_unset(const char *name);
int         alias_expand(const char *cmd, char *out_buf, size_t out_buf_size);
int         alias_list(char *buf, size_t buf_size);
void        alias_destroy(void);


#ifdef __cplusplus
}
#endif

#endif /* ALIAS_ENGINE_H */
