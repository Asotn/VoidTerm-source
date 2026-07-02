/*
 * VoidTerm - Filesystem Utils Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef FS_UTILS_H
#define FS_UTILS_H
#include <stddef.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/statvfs.h>

typedef void (*fs_dir_entry_fn)(const char *name, unsigned char type, void *userdata);

int   fs_exists(const char *path);
int   fs_is_dir(const char *path);
int   fs_is_file(const char *path);
int   fs_mkdirs(const char *path, mode_t mode);
long  fs_file_size(const char *path);
int   fs_copy_file(const char *src, const char *dst);
int   fs_delete_file(const char *path);
int   fs_delete_dir_recursive(const char *path);
char *fs_read_file(const char *path, size_t *out_len);
int   fs_write_file(const char *path, const char *data, size_t len);
int   fs_append_file(const char *path, const char *data, size_t len);
int   fs_chmod(const char *path, mode_t mode);
int   fs_list_dir(const char *path, fs_dir_entry_fn callback, void *userdata);
long  fs_get_free_space(const char *path);
#endif
