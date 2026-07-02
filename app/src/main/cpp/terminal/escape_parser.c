/*
 * VoidTerm - ANSI/VT100 Escape Code Parser
 * Parses terminal escape sequences for color, cursor control, screen ops.
 * Supports: CSI, OSC, DCS, ESC sequences — xterm-256color compatible.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "escape_parser.h"
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-ESC"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// -------------------------------------------------------------------------
// State machine states
// -------------------------------------------------------------------------
typedef enum {
    STATE_GROUND = 0,
    STATE_ESCAPE,
    STATE_CSI_ENTRY,
    STATE_CSI_PARAM,
    STATE_CSI_INTERMEDIATE,
    STATE_CSI_IGNORE,
    STATE_OSC_STRING,
    STATE_DCS_ENTRY,
    STATE_DCS_PASSTHROUGH,
    STATE_SOS_PM_APC_STRING
} parser_state_t;

// -------------------------------------------------------------------------
// escape_parser_init
// -------------------------------------------------------------------------
void escape_parser_init(escape_parser_t *p, escape_action_fn action_fn, void *userdata) {
    if (!p) return;
    memset(p, 0, sizeof(*p));
    p->state      = STATE_GROUND;
    p->action_fn  = action_fn;
    p->userdata   = userdata;
}

// -------------------------------------------------------------------------
// Internal helpers
// -------------------------------------------------------------------------
static void clear_params(escape_parser_t *p) {
    memset(p->params, 0, sizeof(p->params));
    p->param_count  = 0;
    p->current_param = 0;
    p->intermediate_len = 0;
    memset(p->intermediate, 0, sizeof(p->intermediate));
}

static void push_param(escape_parser_t *p) {
    if (p->param_count < ESC_MAX_PARAMS) {
        p->params[p->param_count++] = p->current_param;
    }
    p->current_param = 0;
}

static void dispatch_action(escape_parser_t *p, escape_action_t action,
                             unsigned char c) {
    if (p->action_fn) {
        p->action_fn(p, action, c, p->userdata);
    }
}

// -------------------------------------------------------------------------
// escape_parser_feed
// Feed one byte at a time through the state machine.
// -------------------------------------------------------------------------
void escape_parser_feed(escape_parser_t *p, unsigned char c) {
    // C0 controls (always processed)
    if (c <= 0x1F && c != 0x1B && c != 0x18 && c != 0x1A) {
        if (p->state != STATE_OSC_STRING &&
            p->state != STATE_DCS_PASSTHROUGH &&
            p->state != STATE_SOS_PM_APC_STRING) {
            dispatch_action(p, ESC_ACTION_EXECUTE, c);
            return;
        }
    }

    switch (p->state) {

    // -----------------------------------------------------------------------
    case STATE_GROUND:
        if (c == 0x1B) {
            p->state = STATE_ESCAPE;
            clear_params(p);
        } else {
            dispatch_action(p, ESC_ACTION_PRINT, c);
        }
        break;

    // -----------------------------------------------------------------------
    case STATE_ESCAPE:
        clear_params(p);
        if (c == '[') {
            p->state = STATE_CSI_ENTRY;
        } else if (c == ']') {
            p->state = STATE_OSC_STRING;
            p->osc_len = 0;
        } else if (c == 'P') {
            p->state = STATE_DCS_ENTRY;
        } else if (c >= 0x40 && c <= 0x5F) {
            // ESC Fe — C1 controls
            dispatch_action(p, ESC_ACTION_ESC_DISPATCH, c);
            p->state = STATE_GROUND;
        } else if (c >= 0x60 && c <= 0x7E) {
            // ESC Gs — private use
            dispatch_action(p, ESC_ACTION_ESC_DISPATCH, c);
            p->state = STATE_GROUND;
        } else if (c == 0x1B) {
            // ESC ESC — stay in escape state
        } else {
            dispatch_action(p, ESC_ACTION_ESC_DISPATCH, c);
            p->state = STATE_GROUND;
        }
        break;

    // -----------------------------------------------------------------------
    case STATE_CSI_ENTRY:
        if (c >= '0' && c <= '9') {
            p->current_param = c - '0';
            p->state = STATE_CSI_PARAM;
        } else if (c == ';') {
            push_param(p);
            p->state = STATE_CSI_PARAM;
        } else if (c >= 0x40 && c <= 0x7E) {
            // Final byte
            dispatch_action(p, ESC_ACTION_CSI_DISPATCH, c);
            p->state = STATE_GROUND;
        } else if (c >= 0x20 && c <= 0x2F) {
            // Intermediate
            if (p->intermediate_len < ESC_MAX_INTERMEDIATE) {
                p->intermediate[p->intermediate_len++] = c;
            }
            p->state = STATE_CSI_INTERMEDIATE;
        } else if (c >= 0x3C && c <= 0x3F) {
            // Private parameter
            if (p->intermediate_len < ESC_MAX_INTERMEDIATE) {
                p->intermediate[p->intermediate_len++] = c;
            }
        }
        break;

    // -----------------------------------------------------------------------
    case STATE_CSI_PARAM:
        if (c >= '0' && c <= '9') {
            p->current_param = p->current_param * 10 + (c - '0');
        } else if (c == ';') {
            push_param(p);
        } else if (c >= 0x40 && c <= 0x7E) {
            push_param(p);
            dispatch_action(p, ESC_ACTION_CSI_DISPATCH, c);
            p->state = STATE_GROUND;
        } else if (c >= 0x20 && c <= 0x2F) {
            push_param(p);
            if (p->intermediate_len < ESC_MAX_INTERMEDIATE) {
                p->intermediate[p->intermediate_len++] = c;
            }
            p->state = STATE_CSI_INTERMEDIATE;
        } else if (c == 0x18 || c == 0x1A) {
            dispatch_action(p, ESC_ACTION_EXECUTE, c);
            p->state = STATE_GROUND;
        }
        break;

    // -----------------------------------------------------------------------
    case STATE_CSI_INTERMEDIATE:
        if (c >= 0x40 && c <= 0x7E) {
            dispatch_action(p, ESC_ACTION_CSI_DISPATCH, c);
            p->state = STATE_GROUND;
        } else if (c >= 0x20 && c <= 0x2F) {
            if (p->intermediate_len < ESC_MAX_INTERMEDIATE) {
                p->intermediate[p->intermediate_len++] = c;
            }
        } else {
            p->state = STATE_CSI_IGNORE;
        }
        break;

    // -----------------------------------------------------------------------
    case STATE_CSI_IGNORE:
        if (c >= 0x40 && c <= 0x7E) {
            p->state = STATE_GROUND;
        }
        break;

    // -----------------------------------------------------------------------
    case STATE_OSC_STRING:
        if (c == 0x07 || (c == 0x5C && p->osc_len > 0 &&
                           p->osc_buf[p->osc_len-1] == 0x1B)) {
            // ST (String Terminator) or BEL
            p->osc_buf[p->osc_len] = '\0';
            dispatch_action(p, ESC_ACTION_OSC_DISPATCH, 0);
            p->state = STATE_GROUND;
        } else if (c == 0x1B) {
            if (p->osc_len < ESC_OSC_MAX_LEN) {
                p->osc_buf[p->osc_len++] = c;
            }
        } else {
            if (p->osc_len < ESC_OSC_MAX_LEN) {
                p->osc_buf[p->osc_len++] = c;
            }
        }
        break;

    // -----------------------------------------------------------------------
    case STATE_DCS_ENTRY:
    case STATE_DCS_PASSTHROUGH:
        // Passthrough DCS sequences unchanged
        if (c == 0x1B) {
            p->state = STATE_ESCAPE;
        }
        break;

    // -----------------------------------------------------------------------
    case STATE_SOS_PM_APC_STRING:
        if (c == 0x1B) p->state = STATE_ESCAPE;
        break;

    default:
        p->state = STATE_GROUND;
        break;
    }
}

// -------------------------------------------------------------------------
// escape_parser_feed_str
// Feed a complete string.
// -------------------------------------------------------------------------
void escape_parser_feed_str(escape_parser_t *p, const char *s, size_t len) {
    for (size_t i = 0; i < len; i++) {
        escape_parser_feed(p, (unsigned char)s[i]);
    }
}

// -------------------------------------------------------------------------
// escape_parser_get_param
// Safe parameter accessor with default.
// -------------------------------------------------------------------------
int escape_parser_get_param(const escape_parser_t *p, int index, int def) {
    if (index < 0 || index >= p->param_count) return def;
    return p->params[index] == 0 ? def : p->params[index];
}

// -------------------------------------------------------------------------
// escape_parser_reset
// -------------------------------------------------------------------------
void escape_parser_reset(escape_parser_t *p) {
    p->state = STATE_GROUND;
    clear_params(p);
    p->osc_len = 0;
}

// -------------------------------------------------------------------------
// SGR color decode
// Returns an ansi_color_t struct from an SGR parameter sequence.
// -------------------------------------------------------------------------
ansi_color_t escape_decode_color(const escape_parser_t *p, int *idx) {
    ansi_color_t color = { .mode = COLOR_MODE_NONE, .r = 0, .g = 0, .b = 0, .index = 0 };
    if (!p || !idx || *idx >= p->param_count) return color;

    int v = p->params[(*idx)];

    // 256-color: 38;5;n or 48;5;n
    if ((v == 38 || v == 48) && (*idx + 2) < p->param_count &&
         p->params[(*idx)+1] == 5) {
        color.mode  = COLOR_MODE_256;
        color.index = p->params[(*idx)+2];
        *idx += 2;
        return color;
    }

    // True-color: 38;2;r;g;b or 48;2;r;g;b
    if ((v == 38 || v == 48) && (*idx + 4) < p->param_count &&
         p->params[(*idx)+1] == 2) {
        color.mode = COLOR_MODE_RGB;
        color.r    = (unsigned char)p->params[(*idx)+2];
        color.g    = (unsigned char)p->params[(*idx)+3];
        color.b    = (unsigned char)p->params[(*idx)+4];
        *idx += 4;
        return color;
    }

    // Standard 16-color
    if ((v >= 30 && v <= 37) || (v >= 90 && v <= 97) ||
        (v >= 40 && v <= 47) || (v >= 100 && v <= 107)) {
        color.mode  = COLOR_MODE_16;
        color.index = v;
        return color;
    }

    return color;
}
