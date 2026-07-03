/*
 * VoidTerm - HTTP Client Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef HTTP_CLIENT_H
#define HTTP_CLIENT_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stddef.h>
typedef void (*http_progress_fn)(long downloaded, long total, int pct, void *userdata);
int http_download(const char *url, const char *dest_path,
                   http_progress_fn progress_fn, void *userdata);
int http_fetch_string(const char *url, char *buf, size_t buf_size);

#ifdef __cplusplus
}
#endif

#endif
