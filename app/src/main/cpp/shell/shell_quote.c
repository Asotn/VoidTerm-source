/*
 * VoidTerm - Shell Argument Quoting (implementation)
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
#include "shell_quote.h"
#include <ctype.h>
#include <string.h>

int shell_quote(const char *in, char *out_buf, size_t out_size) {
    if (!in || !out_buf || out_size < 3) return -1;

    size_t pos = 0;
    out_buf[pos++] = '\'';

    for (const char *p = in; *p; p++) {
        if (*p == '\'') {
            /* Close quote, emit an escaped single quote, reopen quote: '\'' */
            if (pos + 4 >= out_size - 1) return -1; /* leave room for closing quote + NUL */
            out_buf[pos++] = '\'';
            out_buf[pos++] = '\\';
            out_buf[pos++] = '\'';
            out_buf[pos++] = '\'';
        } else {
            if (pos + 1 >= out_size - 1) return -1;
            out_buf[pos++] = *p;
        }
    }

    if (pos + 1 >= out_size) return -1;
    out_buf[pos++] = '\'';
    out_buf[pos] = '\0';
    return 0;
}

int shell_is_safe_token(const char *in) {
    if (!in || !*in) return 0;
    for (const char *p = in; *p; p++) {
        unsigned char c = (unsigned char)*p;
        if (isalnum(c) || c == '-' || c == '_' || c == '.' || c == '/' || c == '+' || c == ':') {
            continue;
        }
        return 0;
    }
    return 1;
}
