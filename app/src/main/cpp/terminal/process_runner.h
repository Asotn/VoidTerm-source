/*
 * VoidTerm - Process Runner Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef PROCESS_RUNNER_H
#define PROCESS_RUNNER_H

#ifdef __cplusplus
extern "C" {
#endif


#include <stddef.h>

int process_run(const char *path, const char *const argv[],
                const char *const envp[],
                char *out_buf, size_t out_buf_size,
                int timeout_ms);

int process_run_shell(const char *cmd, char *out_buf, size_t out_buf_size);

int process_run_proot(const char *proot_bin, const char *rootfs,
                       const char *cmd,
                       char *out_buf, size_t out_buf_size);

int process_check_exists(const char *path);

int process_get_output_line(const char *cmd, char *line_buf, size_t line_buf_size);


#ifdef __cplusplus
}
#endif

#endif /* PROCESS_RUNNER_H */
