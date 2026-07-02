/*
 * VoidTerm - Process Runner
 * Executes shell processes, manages environment, handles exit status.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "process_runner.h"
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/wait.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-Proc"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// -------------------------------------------------------------------------
// process_run
// Run a process, capture stdout/stderr into out_buf.
// Returns exit code.
// -------------------------------------------------------------------------
int process_run(const char *path, const char *const argv[],
                const char *const envp[],
                char *out_buf, size_t out_buf_size,
                int timeout_ms) {

    int pipefd[2];
    if (pipe(pipefd) < 0) {
        LOGE("pipe failed: %s", strerror(errno));
        return -1;
    }

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork failed: %s", strerror(errno));
        close(pipefd[0]);
        close(pipefd[1]);
        return -1;
    }

    if (pid == 0) {
        // Child
        close(pipefd[0]);
        dup2(pipefd[1], STDOUT_FILENO);
        dup2(pipefd[1], STDERR_FILENO);
        close(pipefd[1]);

        // Redirect stdin from /dev/null
        int devnull = open("/dev/null", O_RDONLY);
        if (devnull >= 0) {
            dup2(devnull, STDIN_FILENO);
            close(devnull);
        }

        if (envp) {
            execve(path, (char *const *)argv, (char *const *)envp);
        } else {
            execv(path, (char *const *)argv);
        }
        _exit(127);

    } else {
        // Parent
        close(pipefd[1]);

        size_t total = 0;
        char tmp[4096];
        ssize_t n;

        while ((n = read(pipefd[0], tmp, sizeof(tmp))) > 0) {
            if (out_buf && total + (size_t)n < out_buf_size) {
                memcpy(out_buf + total, tmp, (size_t)n);
                total += (size_t)n;
            }
        }

        close(pipefd[0]);

        if (out_buf && total < out_buf_size) {
            out_buf[total] = '\0';
        }

        int status;
        waitpid(pid, &status, 0);
        return WIFEXITED(status) ? WEXITSTATUS(status) : -1;
    }
}

// -------------------------------------------------------------------------
// process_run_shell
// Run a command string through /system/bin/sh.
// -------------------------------------------------------------------------
int process_run_shell(const char *cmd, char *out_buf, size_t out_buf_size) {
    const char *argv[] = { "sh", "-c", cmd, NULL };
    return process_run("/system/bin/sh", argv, NULL, out_buf, out_buf_size, 30000);
}

// -------------------------------------------------------------------------
// process_run_proot
// Run a command inside the proot environment.
// -------------------------------------------------------------------------
int process_run_proot(const char *proot_bin, const char *rootfs,
                       const char *cmd,
                       char *out_buf, size_t out_buf_size) {
    const char *argv[] = {
        proot_bin,
        "--link2symlink",
        "--root-id",
        "-r", rootfs,
        "-w", "/root",
        "--bind=/proc",
        "--bind=/dev",
        "--bind=/sys",
        "/bin/bash", "-c", cmd,
        NULL
    };
    return process_run(proot_bin, argv, NULL, out_buf, out_buf_size, 60000);
}

// -------------------------------------------------------------------------
// process_check_exists
// Returns 1 if binary exists and is executable.
// -------------------------------------------------------------------------
int process_check_exists(const char *path) {
    return access(path, X_OK) == 0;
}

// -------------------------------------------------------------------------
// process_get_output_line
// Runs a command, returns first line of output in line_buf.
// -------------------------------------------------------------------------
int process_get_output_line(const char *cmd, char *line_buf, size_t line_buf_size) {
    char out[4096] = {0};
    int ret = process_run_shell(cmd, out, sizeof(out));
    if (ret != 0) return ret;

    // Copy up to first newline
    size_t i = 0;
    while (i < sizeof(out) - 1 && out[i] && out[i] != '\n' && i < line_buf_size - 1) {
        line_buf[i] = out[i];
        i++;
    }
    line_buf[i] = '\0';
    return 0;
}
