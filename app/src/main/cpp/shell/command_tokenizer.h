/*
 * VoidTerm - Command Tokenizer Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef COMMAND_TOKENIZER_H
#define COMMAND_TOKENIZER_H

#ifdef __cplusplus
extern "C" {
#endif


#define MAX_TOKENS      256
#define TOKEN_MAX_LEN   4096

typedef enum {
    TOKEN_WORD = 0,
    TOKEN_PIPE,
    TOKEN_AND,
    TOKEN_OR,
    TOKEN_BACKGROUND,
    TOKEN_SEMICOLON,
    TOKEN_REDIR_IN,
    TOKEN_REDIR_OUT,
    TOKEN_REDIR_APPEND,
    TOKEN_REDIR_BOTH
} token_type_t;

typedef struct {
    token_type_t  type;
    char         *value;
    int           len;
} token_t;

typedef struct {
    token_t tokens[MAX_TOKENS];
    int     count;
} token_list_t;

void        token_list_init(token_list_t *tl);
void        token_list_destroy(token_list_t *tl);
int         tokenize(const char *input, token_list_t *tl);
const char *token_type_name(token_type_t type);
const char *token_list_first_word(const token_list_t *tl);


#ifdef __cplusplus
}
#endif

#endif /* COMMAND_TOKENIZER_H */
