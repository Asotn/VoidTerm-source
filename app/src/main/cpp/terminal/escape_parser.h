/*
 * VoidTerm - Escape Parser Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef ESCAPE_PARSER_H
#define ESCAPE_PARSER_H

#ifdef __cplusplus
extern "C" {
#endif


#include <stddef.h>

#define ESC_MAX_PARAMS       32
#define ESC_MAX_INTERMEDIATE 4
#define ESC_OSC_MAX_LEN      512

typedef enum {
    ESC_ACTION_PRINT = 0,
    ESC_ACTION_EXECUTE,
    ESC_ACTION_ESC_DISPATCH,
    ESC_ACTION_CSI_DISPATCH,
    ESC_ACTION_OSC_DISPATCH,
    ESC_ACTION_HOOK,
    ESC_ACTION_PUT,
    ESC_ACTION_UNHOOK
} escape_action_t;

typedef enum {
    COLOR_MODE_NONE = 0,
    COLOR_MODE_16,
    COLOR_MODE_256,
    COLOR_MODE_RGB
} color_mode_t;

typedef struct {
    color_mode_t   mode;
    unsigned char  r, g, b;
    int            index;
} ansi_color_t;

struct escape_parser;
typedef void (*escape_action_fn)(struct escape_parser *p, escape_action_t action,
                                  unsigned char c, void *userdata);

typedef struct escape_parser {
    int              state;
    int              params[ESC_MAX_PARAMS];
    int              param_count;
    int              current_param;
    char             intermediate[ESC_MAX_INTERMEDIATE];
    int              intermediate_len;
    char             osc_buf[ESC_OSC_MAX_LEN + 1];
    int              osc_len;
    escape_action_fn action_fn;
    void            *userdata;
} escape_parser_t;

void         escape_parser_init(escape_parser_t *p, escape_action_fn fn, void *ud);
void         escape_parser_feed(escape_parser_t *p, unsigned char c);
void         escape_parser_feed_str(escape_parser_t *p, const char *s, size_t len);
int          escape_parser_get_param(const escape_parser_t *p, int index, int def);
void         escape_parser_reset(escape_parser_t *p);
ansi_color_t escape_decode_color(const escape_parser_t *p, int *idx);


#ifdef __cplusplus
}
#endif

#endif /* ESCAPE_PARSER_H */
