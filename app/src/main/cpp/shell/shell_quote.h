/*
 * VoidTerm - Shell Argument Quoting
 * Security-hardening helper: safely single-quotes untrusted strings before
 * they are interpolated into a shell command line passed to system()/popen()
 * or a proot invocation. Prevents shell metacharacter / command injection
 * (e.g. "'; rm -rf / #", "$(...)", backticks, "&&", "|", etc.) from being
 * interpreted by the shell when values originate from network responses,
 * mirror URLs, package names, or other data that isn't fully trusted.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
#ifndef VOIDTERM_SHELL_QUOTE_H
#define VOIDTERM_SHELL_QUOTE_H

#ifdef __cplusplus
extern "C" {
#endif


#include <stddef.h>

/*
 * shell_quote
 * Wraps `in` in single quotes, escaping any embedded single quotes using
 * the standard '\'' technique, and writes the result into out_buf.
 * Returns 0 on success, -1 if out_buf is too small or arguments are invalid.
 */
int shell_quote(const char *in, char *out_buf, size_t out_size);

/*
 * shell_is_safe_token
 * Returns 1 if `in` contains only characters that are always safe to place
 * unquoted in a shell command (alphanumerics, '-', '_', '.', '/', '+', ':').
 * Useful for fast-path validation of package names / simple identifiers
 * before they are used to build a command string.
 */
int shell_is_safe_token(const char *in);


#ifdef __cplusplus
}
#endif

#endif
