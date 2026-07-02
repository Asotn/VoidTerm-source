/*
 * VoidTerm - PTY Manager Header
 * Developer: Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef PTY_MANAGER_H
#define PTY_MANAGER_H

#include <sys/types.h>

#define MAX_PTY_SESSIONS 8
#define PTY_READ_BUFFER_SIZE (64 * 1024)

typedef struct {
    int    master_fd;
    int    slave_fd;
    pid_t  pid;
    int    active;
    int    cols;
    int    rows;
    char   slave_name[64];
} pty_session_t;

int     pty_manager_init(void);
int     pty_open_session(const char *shell_path,
                         const char *const argv[],
                         const char *const envp[],
                         const char *cwd,
                         int cols, int rows);
ssize_t pty_write(int session, const char *buf, size_t len);
ssize_t pty_read(int session, char *buf, size_t len);
int     pty_resize(int session, int cols, int rows);
int     pty_close_session(int session);
int     pty_get_master_fd(int session);
int     pty_is_alive(int session);
int     pty_send_signal(int session, int sig);
void    pty_manager_destroy(void);

#endif /* PTY_MANAGER_H */
