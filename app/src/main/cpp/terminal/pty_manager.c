/*
 * VoidTerm - PTY Manager
 * Handles pseudo-terminal creation, fork/exec of shell processes,
 * I/O multiplexing, and window-size change signals.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "pty_manager.h"
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <signal.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-PTY"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// -------------------------------------------------------------------------
// Internal state
// -------------------------------------------------------------------------
static pty_session_t g_sessions[MAX_PTY_SESSIONS];
static int           g_session_count = 0;

// -------------------------------------------------------------------------
// pty_manager_init
// -------------------------------------------------------------------------
int pty_manager_init(void) {
    memset(g_sessions, 0, sizeof(g_sessions));
    for (int i = 0; i < MAX_PTY_SESSIONS; i++) {
        g_sessions[i].master_fd = -1;
        g_sessions[i].slave_fd  = -1;
        g_sessions[i].pid       = -1;
        g_sessions[i].active    = 0;
    }
    LOGI("PTY manager initialized. Max sessions: %d", MAX_PTY_SESSIONS);
    return 0;
}

// -------------------------------------------------------------------------
// pty_open_session
// Creates a new PTY pair and forks a shell.
// Returns session index on success, -1 on failure.
// -------------------------------------------------------------------------
int pty_open_session(const char *shell_path,
                     const char *const argv[],
                     const char *const envp[],
                     const char *cwd,
                     int cols, int rows) {

    if (g_session_count >= MAX_PTY_SESSIONS) {
        LOGE("Max sessions reached (%d)", MAX_PTY_SESSIONS);
        return -1;
    }

    // Find a free slot
    int slot = -1;
    for (int i = 0; i < MAX_PTY_SESSIONS; i++) {
        if (!g_sessions[i].active) {
            slot = i;
            break;
        }
    }
    if (slot == -1) {
        LOGE("No free session slot");
        return -1;
    }

    pty_session_t *s = &g_sessions[slot];

    // Open master PTY
    s->master_fd = posix_openpt(O_RDWR | O_NOCTTY | O_CLOEXEC);
    if (s->master_fd < 0) {
        LOGE("posix_openpt failed: %s", strerror(errno));
        return -1;
    }

    if (grantpt(s->master_fd) < 0 || unlockpt(s->master_fd) < 0) {
        LOGE("grantpt/unlockpt failed: %s", strerror(errno));
        close(s->master_fd);
        s->master_fd = -1;
        return -1;
    }

    // Get slave PTY name
    char *slave_name = ptsname(s->master_fd);
    if (!slave_name) {
        LOGE("ptsname failed: %s", strerror(errno));
        close(s->master_fd);
        s->master_fd = -1;
        return -1;
    }

    strncpy(s->slave_name, slave_name, sizeof(s->slave_name) - 1);
    LOGI("PTY pair: master=%d slave=%s", s->master_fd, s->slave_name);

    // Set initial window size
    struct winsize ws;
    ws.ws_col    = (unsigned short)cols;
    ws.ws_row    = (unsigned short)rows;
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;
    ioctl(s->master_fd, TIOCSWINSZ, &ws);

    // Fork
    s->pid = fork();

    if (s->pid < 0) {
        LOGE("fork failed: %s", strerror(errno));
        close(s->master_fd);
        s->master_fd = -1;
        return -1;

    } else if (s->pid == 0) {
        // --- CHILD ---
        // Create new session
        if (setsid() < 0) _exit(1);

        // Open slave PTY
        int slave_fd = open(s->slave_name, O_RDWR);
        if (slave_fd < 0) _exit(1);

        // Set as controlling terminal
        ioctl(slave_fd, TIOCSCTTY, 0);

        // Redirect stdin/stdout/stderr to slave
        dup2(slave_fd, STDIN_FILENO);
        dup2(slave_fd, STDOUT_FILENO);
        dup2(slave_fd, STDERR_FILENO);
        if (slave_fd > STDERR_FILENO) close(slave_fd);
        close(s->master_fd);

        // Set terminal attributes
        struct termios tios;
        tcgetattr(STDIN_FILENO, &tios);
        tios.c_iflag |= ICRNL | IXON;
        tios.c_oflag |= OPOST | ONLCR;
        tios.c_lflag |= ECHO | ECHOE | ECHOK | ICANON | ISIG;
        tcsetattr(STDIN_FILENO, TCSANOW, &tios);

        // Change to working directory
        if (cwd && strlen(cwd) > 0) {
            if (chdir(cwd) < 0) {
                chdir("/data/data/com.asotn.voidterm/files/home");
            }
        }

        // Exec shell
        if (envp) {
            execve(shell_path, (char *const *)argv, (char *const *)envp);
        } else {
            execv(shell_path, (char *const *)argv);
        }

        // Exec failed
        LOGE("execve failed: %s -> %s", shell_path, strerror(errno));
        _exit(127);

    } else {
        // --- PARENT ---
        s->slave_fd = -1; // parent doesn't keep slave open
        s->active   = 1;
        s->cols     = cols;
        s->rows     = rows;
        g_session_count++;
        LOGI("Session %d started, pid=%d", slot, s->pid);
        return slot;
    }
}

// -------------------------------------------------------------------------
// pty_write
// Write data to the master PTY (→ shell stdin).
// -------------------------------------------------------------------------
ssize_t pty_write(int session, const char *buf, size_t len) {
    if (session < 0 || session >= MAX_PTY_SESSIONS) return -1;
    pty_session_t *s = &g_sessions[session];
    if (!s->active || s->master_fd < 0) return -1;

    ssize_t written = 0;
    while ((size_t)written < len) {
        ssize_t n = write(s->master_fd, buf + written, len - (size_t)written);
        if (n < 0) {
            if (errno == EINTR) continue;
            LOGE("pty_write error: %s", strerror(errno));
            return -1;
        }
        written += n;
    }
    return written;
}

// -------------------------------------------------------------------------
// pty_read
// Read data from the master PTY (shell stdout/stderr).
// -------------------------------------------------------------------------
ssize_t pty_read(int session, char *buf, size_t len) {
    if (session < 0 || session >= MAX_PTY_SESSIONS) return -1;
    pty_session_t *s = &g_sessions[session];
    if (!s->active || s->master_fd < 0) return -1;

    ssize_t n;
    do {
        n = read(s->master_fd, buf, len);
    } while (n < 0 && errno == EINTR);

    if (n < 0) {
        if (errno == EIO) {
            // Shell exited
            LOGI("Session %d: shell exited (EIO)", session);
        } else {
            LOGE("pty_read error: %s", strerror(errno));
        }
    }
    return n;
}

// -------------------------------------------------------------------------
// pty_resize
// Send SIGWINCH after updating window size.
// -------------------------------------------------------------------------
int pty_resize(int session, int cols, int rows) {
    if (session < 0 || session >= MAX_PTY_SESSIONS) return -1;
    pty_session_t *s = &g_sessions[session];
    if (!s->active || s->master_fd < 0) return -1;

    struct winsize ws;
    ws.ws_col    = (unsigned short)cols;
    ws.ws_row    = (unsigned short)rows;
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;

    if (ioctl(s->master_fd, TIOCSWINSZ, &ws) < 0) {
        LOGE("TIOCSWINSZ failed: %s", strerror(errno));
        return -1;
    }

    if (s->pid > 0) kill(s->pid, SIGWINCH);

    s->cols = cols;
    s->rows = rows;
    LOGD("Session %d resized to %dx%d", session, cols, rows);
    return 0;
}

// -------------------------------------------------------------------------
// pty_close_session
// -------------------------------------------------------------------------
int pty_close_session(int session) {
    if (session < 0 || session >= MAX_PTY_SESSIONS) return -1;
    pty_session_t *s = &g_sessions[session];
    if (!s->active) return -1;

    if (s->pid > 0) {
        kill(s->pid, SIGHUP);
        int status;
        waitpid(s->pid, &status, WNOHANG);
    }

    if (s->master_fd >= 0) {
        close(s->master_fd);
        s->master_fd = -1;
    }

    s->active = 0;
    s->pid    = -1;
    g_session_count--;
    LOGI("Session %d closed", session);
    return 0;
}

// -------------------------------------------------------------------------
// pty_get_master_fd
// -------------------------------------------------------------------------
int pty_get_master_fd(int session) {
    if (session < 0 || session >= MAX_PTY_SESSIONS) return -1;
    return g_sessions[session].master_fd;
}

// -------------------------------------------------------------------------
// pty_is_alive
// Non-blocking waitpid to check if child is still running.
// -------------------------------------------------------------------------
int pty_is_alive(int session) {
    if (session < 0 || session >= MAX_PTY_SESSIONS) return 0;
    pty_session_t *s = &g_sessions[session];
    if (!s->active || s->pid < 0) return 0;

    int status;
    pid_t result = waitpid(s->pid, &status, WNOHANG);
    if (result == 0) return 1;   // still running
    if (result < 0)  return 0;   // error

    s->active = 0;
    g_session_count--;
    LOGI("Session %d: process exited with status %d", session, WEXITSTATUS(status));
    return 0;
}

// -------------------------------------------------------------------------
// pty_send_signal
// -------------------------------------------------------------------------
int pty_send_signal(int session, int sig) {
    if (session < 0 || session >= MAX_PTY_SESSIONS) return -1;
    pty_session_t *s = &g_sessions[session];
    if (!s->active || s->pid < 0) return -1;
    return kill(s->pid, sig);
}

// -------------------------------------------------------------------------
// pty_manager_destroy
// -------------------------------------------------------------------------
void pty_manager_destroy(void) {
    for (int i = 0; i < MAX_PTY_SESSIONS; i++) {
        if (g_sessions[i].active) {
            pty_close_session(i);
        }
    }
    LOGI("PTY manager destroyed");
}
