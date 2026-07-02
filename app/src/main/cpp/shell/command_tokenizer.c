/*
 * VoidTerm - Command Tokenizer
 * Tokenizes shell command strings respecting quotes, escape chars, pipes,
 * redirections, and semicolons.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "command_tokenizer.h"
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-Tok"

// -------------------------------------------------------------------------
// token_list_init
// -------------------------------------------------------------------------
void token_list_init(token_list_t *tl) {
    if (!tl) return;
    memset(tl, 0, sizeof(*tl));
}

// -------------------------------------------------------------------------
// token_list_destroy
// -------------------------------------------------------------------------
void token_list_destroy(token_list_t *tl) {
    if (!tl) return;
    for (int i = 0; i < tl->count; i++) {
        free(tl->tokens[i].value);
    }
    memset(tl, 0, sizeof(*tl));
}

// -------------------------------------------------------------------------
// Internal: add token
// -------------------------------------------------------------------------
static int add_token(token_list_t *tl, token_type_t type,
                      const char *val, int len) {
    if (tl->count >= MAX_TOKENS) return -1;

    token_t *t = &tl->tokens[tl->count];
    t->type    = type;
    t->value   = malloc((size_t)(len + 1));
    if (!t->value) return -1;
    memcpy(t->value, val, (size_t)len);
    t->value[len] = '\0';
    t->len = len;
    tl->count++;
    return 0;
}

// -------------------------------------------------------------------------
// tokenize
// Returns 0 on success.
// -------------------------------------------------------------------------
int tokenize(const char *input, token_list_t *tl) {
    if (!input || !tl) return -1;
    token_list_init(tl);

    const char *p   = input;
    char buf[TOKEN_MAX_LEN];
    int  buf_len = 0;
    int  in_single_quote = 0;
    int  in_double_quote = 0;

    while (*p) {
        char c = *p;

        if (in_single_quote) {
            if (c == '\'') {
                in_single_quote = 0;
            } else {
                if (buf_len < TOKEN_MAX_LEN - 1) buf[buf_len++] = c;
            }
            p++;
            continue;
        }

        if (in_double_quote) {
            if (c == '"') {
                in_double_quote = 0;
            } else if (c == '\\' && *(p+1)) {
                p++;
                if (buf_len < TOKEN_MAX_LEN - 1) buf[buf_len++] = *p;
            } else {
                if (buf_len < TOKEN_MAX_LEN - 1) buf[buf_len++] = c;
            }
            p++;
            continue;
        }

        // Escape
        if (c == '\\' && *(p+1)) {
            p++;
            if (buf_len < TOKEN_MAX_LEN - 1) buf[buf_len++] = *p;
            p++;
            continue;
        }

        // Quotes
        if (c == '\'') { in_single_quote = 1; p++; continue; }
        if (c == '"')  { in_double_quote = 1; p++; continue; }

        // Whitespace: flush current token
        if (isspace((unsigned char)c)) {
            if (buf_len > 0) {
                add_token(tl, TOKEN_WORD, buf, buf_len);
                buf_len = 0;
            }
            p++;
            continue;
        }

        // Operators
        if (c == '|') {
            if (buf_len > 0) { add_token(tl, TOKEN_WORD, buf, buf_len); buf_len = 0; }
            if (*(p+1) == '|') {
                add_token(tl, TOKEN_OR, "||", 2);
                p += 2;
            } else {
                add_token(tl, TOKEN_PIPE, "|", 1);
                p++;
            }
            continue;
        }

        if (c == '&') {
            if (buf_len > 0) { add_token(tl, TOKEN_WORD, buf, buf_len); buf_len = 0; }
            if (*(p+1) == '&') {
                add_token(tl, TOKEN_AND, "&&", 2);
                p += 2;
            } else if (*(p+1) == '>') {
                add_token(tl, TOKEN_REDIR_BOTH, "&>", 2);
                p += 2;
            } else {
                add_token(tl, TOKEN_BACKGROUND, "&", 1);
                p++;
            }
            continue;
        }

        if (c == ';') {
            if (buf_len > 0) { add_token(tl, TOKEN_WORD, buf, buf_len); buf_len = 0; }
            add_token(tl, TOKEN_SEMICOLON, ";", 1);
            p++;
            continue;
        }

        if (c == '>') {
            if (buf_len > 0) { add_token(tl, TOKEN_WORD, buf, buf_len); buf_len = 0; }
            if (*(p+1) == '>') {
                add_token(tl, TOKEN_REDIR_APPEND, ">>", 2);
                p += 2;
            } else {
                add_token(tl, TOKEN_REDIR_OUT, ">", 1);
                p++;
            }
            continue;
        }

        if (c == '<') {
            if (buf_len > 0) { add_token(tl, TOKEN_WORD, buf, buf_len); buf_len = 0; }
            add_token(tl, TOKEN_REDIR_IN, "<", 1);
            p++;
            continue;
        }

        // Regular character
        if (buf_len < TOKEN_MAX_LEN - 1) buf[buf_len++] = c;
        p++;
    }

    if (buf_len > 0) {
        add_token(tl, TOKEN_WORD, buf, buf_len);
    }

    return 0;
}

// -------------------------------------------------------------------------
// token_type_name
// -------------------------------------------------------------------------
const char *token_type_name(token_type_t type) {
    switch (type) {
    case TOKEN_WORD:        return "WORD";
    case TOKEN_PIPE:        return "PIPE";
    case TOKEN_AND:         return "AND";
    case TOKEN_OR:          return "OR";
    case TOKEN_BACKGROUND:  return "BG";
    case TOKEN_SEMICOLON:   return "SEMICOLON";
    case TOKEN_REDIR_IN:    return "REDIR_IN";
    case TOKEN_REDIR_OUT:   return "REDIR_OUT";
    case TOKEN_REDIR_APPEND:return "REDIR_APPEND";
    case TOKEN_REDIR_BOTH:  return "REDIR_BOTH";
    default:                return "UNKNOWN";
    }
}

// -------------------------------------------------------------------------
// first_word
// Returns the first WORD token value, or empty string.
// -------------------------------------------------------------------------
const char *token_list_first_word(const token_list_t *tl) {
    if (!tl || tl->count == 0) return "";
    for (int i = 0; i < tl->count; i++) {
        if (tl->tokens[i].type == TOKEN_WORD) return tl->tokens[i].value;
    }
    return "";
}
