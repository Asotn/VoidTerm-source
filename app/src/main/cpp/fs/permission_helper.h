/*
 * VoidTerm - Permission Helper Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef PERMISSION_HELPER_H
#define PERMISSION_HELPER_H

#ifdef __cplusplus
extern "C" {
#endif

int perm_can_read(const char *path);
int perm_can_write(const char *path);
int perm_can_execute(const char *path);
int perm_get_mode_string(const char *path, char out[11]);
int perm_make_executable(const char *path);
int perm_check_sdcard(void);
int perm_check_sdcard_write(void);
int perm_is_root(void);
int perm_get_uid(void);

#ifdef __cplusplus
}
#endif

#endif
