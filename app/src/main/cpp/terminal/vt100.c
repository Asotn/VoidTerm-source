/*
 * VoidTerm - VT100/VT220 Terminal Emulator
 * Implements a screen buffer, cursor, SGR attributes.
 * Used internally to track terminal state for rendering.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "vt100.h"
#include "escape_parser.h"
#include <string.h>
#include <stdlib.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-VT100"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// -------------------------------------------------------------------------
// Forward declarations
// -------------------------------------------------------------------------
static void vt100_action(escape_parser_t *ep, escape_action_t action,
                          unsigned char c, void *ud);
static void handle_csi(vt100_t *vt, unsigned char final);
static void handle_sgr(vt100_t *vt);
static void screen_scroll_up(vt100_t *vt, int top, int bot, int n);
static void screen_scroll_down(vt100_t *vt, int top, int bot, int n);
static void cursor_move(vt100_t *vt, int row, int col);
static void erase_in_display(vt100_t *vt, int mode);
static void erase_in_line(vt100_t *vt, int mode);

// -------------------------------------------------------------------------
// vt100_init
// -------------------------------------------------------------------------
int vt100_init(vt100_t *vt, int cols, int rows) {
    if (!vt || cols <= 0 || rows <= 0) return -1;
    memset(vt, 0, sizeof(*vt));

    vt->cols = cols;
    vt->rows = rows;

    // Allocate screen buffer: rows * cols cells
    vt->screen = calloc((size_t)(rows * cols), sizeof(vt100_cell_t));
    if (!vt->screen) return -1;

    // Allocate alt screen buffer
    vt->alt_screen = calloc((size_t)(rows * cols), sizeof(vt100_cell_t));
    if (!vt->alt_screen) { free(vt->screen); return -1; }

    vt->cursor_row = 0;
    vt->cursor_col = 0;
    vt->scroll_top = 0;
    vt->scroll_bot = rows - 1;

    // Default SGR: white on black
    vt->current_attr.fg.mode  = COLOR_MODE_16;
    vt->current_attr.fg.index = 37; // white
    vt->current_attr.bg.mode  = COLOR_MODE_16;
    vt->current_attr.bg.index = 40; // black
    vt->current_attr.bold      = 0;
    vt->current_attr.underline = 0;
    vt->current_attr.reverse   = 0;

    escape_parser_init(&vt->parser, vt100_action, vt);
    LOGI("VT100 initialized: %dx%d", cols, rows);
    return 0;
}

// -------------------------------------------------------------------------
// vt100_destroy
// -------------------------------------------------------------------------
void vt100_destroy(vt100_t *vt) {
    if (!vt) return;
    free(vt->screen);
    free(vt->alt_screen);
    vt->screen = vt->alt_screen = NULL;
}

// -------------------------------------------------------------------------
// vt100_feed
// -------------------------------------------------------------------------
void vt100_feed(vt100_t *vt, const char *data, size_t len) {
    escape_parser_feed_str(&vt->parser, data, len);
}

// -------------------------------------------------------------------------
// vt100_action callback
// -------------------------------------------------------------------------
static void vt100_action(escape_parser_t *ep, escape_action_t action,
                          unsigned char c, void *ud) {
    vt100_t *vt = (vt100_t *)ud;

    switch (action) {

    case ESC_ACTION_PRINT: {
        // Write character at cursor position
        if (vt->cursor_col >= vt->cols) {
            // Wrap
            vt->cursor_col = 0;
            if (vt->cursor_row < vt->scroll_bot) {
                vt->cursor_row++;
            } else {
                screen_scroll_up(vt, vt->scroll_top, vt->scroll_bot, 1);
            }
        }
        int idx = vt->cursor_row * vt->cols + vt->cursor_col;
        vt->screen[idx].ch   = (unsigned int)c;
        vt->screen[idx].attr = vt->current_attr;
        vt->cursor_col++;
        vt->dirty = 1;
        break;
    }

    case ESC_ACTION_EXECUTE:
        switch (c) {
        case '\r': vt->cursor_col = 0; break;
        case '\n':
        case '\v':
        case '\f':
            if (vt->cursor_row < vt->scroll_bot) {
                vt->cursor_row++;
            } else {
                screen_scroll_up(vt, vt->scroll_top, vt->scroll_bot, 1);
            }
            vt->dirty = 1;
            break;
        case '\b':
            if (vt->cursor_col > 0) vt->cursor_col--;
            break;
        case '\t': {
            int next = (vt->cursor_col | 7) + 1;
            vt->cursor_col = next < vt->cols ? next : vt->cols - 1;
            break;
        }
        case 0x07: /* BEL — ignored */ break;
        }
        break;

    case ESC_ACTION_CSI_DISPATCH:
        handle_csi(vt, c);
        break;

    case ESC_ACTION_ESC_DISPATCH:
        switch (c) {
        case 'M': // Reverse index
            if (vt->cursor_row > vt->scroll_top) {
                vt->cursor_row--;
            } else {
                screen_scroll_down(vt, vt->scroll_top, vt->scroll_bot, 1);
            }
            vt->dirty = 1;
            break;
        case '7': // Save cursor
            vt->saved_row = vt->cursor_row;
            vt->saved_col = vt->cursor_col;
            break;
        case '8': // Restore cursor
            vt->cursor_row = vt->saved_row;
            vt->cursor_col = vt->saved_col;
            break;
        }
        break;

    default:
        break;
    }
}

// -------------------------------------------------------------------------
// handle_csi
// -------------------------------------------------------------------------
static void handle_csi(vt100_t *vt, unsigned char final) {
    escape_parser_t *ep = &vt->parser;
    int p0 = escape_parser_get_param(ep, 0, 1);
    int p1 = escape_parser_get_param(ep, 1, 1);

    switch (final) {
    case 'A': cursor_move(vt, vt->cursor_row - p0, vt->cursor_col); break;
    case 'B': cursor_move(vt, vt->cursor_row + p0, vt->cursor_col); break;
    case 'C': cursor_move(vt, vt->cursor_row, vt->cursor_col + p0); break;
    case 'D': cursor_move(vt, vt->cursor_row, vt->cursor_col - p0); break;
    case 'E': cursor_move(vt, vt->cursor_row + p0, 0); break;
    case 'F': cursor_move(vt, vt->cursor_row - p0, 0); break;
    case 'G': cursor_move(vt, vt->cursor_row, p0 - 1); break;
    case 'H':
    case 'f': cursor_move(vt, p0 - 1, p1 - 1); break;
    case 'J': erase_in_display(vt, escape_parser_get_param(ep, 0, 0)); break;
    case 'K': erase_in_line(vt, escape_parser_get_param(ep, 0, 0)); break;
    case 'L': screen_scroll_down(vt, vt->cursor_row, vt->scroll_bot, p0); break;
    case 'M': screen_scroll_up(vt, vt->cursor_row, vt->scroll_bot, p0); break;
    case 'P': { // Delete chars
        int row = vt->cursor_row * vt->cols;
        int end = vt->cols - p0;
        memmove(&vt->screen[row + vt->cursor_col],
                &vt->screen[row + vt->cursor_col + p0],
                (size_t)(end - vt->cursor_col) * sizeof(vt100_cell_t));
        for (int i = end; i < vt->cols; i++) {
            vt->screen[row + i].ch = ' ';
        }
        break;
    }
    case 'S': screen_scroll_up(vt, vt->scroll_top, vt->scroll_bot, p0); break;
    case 'T': screen_scroll_down(vt, vt->scroll_top, vt->scroll_bot, p0); break;
    case 'd': cursor_move(vt, p0 - 1, vt->cursor_col); break;
    case 'm': handle_sgr(vt); break;
    case 'r': // Set scroll region
        vt->scroll_top = p0 - 1;
        vt->scroll_bot = p1 - 1;
        if (vt->scroll_top < 0) vt->scroll_top = 0;
        if (vt->scroll_bot >= vt->rows) vt->scroll_bot = vt->rows - 1;
        cursor_move(vt, 0, 0);
        break;
    case 's': // Save cursor
        vt->saved_row = vt->cursor_row;
        vt->saved_col = vt->cursor_col;
        break;
    case 'u': // Restore cursor
        vt->cursor_row = vt->saved_row;
        vt->cursor_col = vt->saved_col;
        break;
    }
    vt->dirty = 1;
}

// -------------------------------------------------------------------------
// handle_sgr
// -------------------------------------------------------------------------
static void handle_sgr(vt100_t *vt) {
    escape_parser_t *ep = &vt->parser;
    int n = ep->param_count == 0 ? 1 : ep->param_count;

    for (int i = 0; i < n; i++) {
        int v = escape_parser_get_param(ep, i, 0);
        switch (v) {
        case 0:  // Reset
            memset(&vt->current_attr, 0, sizeof(vt->current_attr));
            vt->current_attr.fg.mode  = COLOR_MODE_16;
            vt->current_attr.fg.index = 37;
            vt->current_attr.bg.mode  = COLOR_MODE_16;
            vt->current_attr.bg.index = 40;
            break;
        case 1:  vt->current_attr.bold      = 1; break;
        case 3:  vt->current_attr.italic    = 1; break;
        case 4:  vt->current_attr.underline = 1; break;
        case 5:  vt->current_attr.blink     = 1; break;
        case 7:  vt->current_attr.reverse   = 1; break;
        case 22: vt->current_attr.bold      = 0; break;
        case 23: vt->current_attr.italic    = 0; break;
        case 24: vt->current_attr.underline = 0; break;
        case 25: vt->current_attr.blink     = 0; break;
        case 27: vt->current_attr.reverse   = 0; break;
        default:
            if (v >= 30 && v <= 37) {
                vt->current_attr.fg.mode  = COLOR_MODE_16;
                vt->current_attr.fg.index = v;
            } else if (v >= 40 && v <= 47) {
                vt->current_attr.bg.mode  = COLOR_MODE_16;
                vt->current_attr.bg.index = v;
            } else if (v >= 90 && v <= 97) {
                vt->current_attr.fg.mode  = COLOR_MODE_16;
                vt->current_attr.fg.index = v;
            } else if (v >= 100 && v <= 107) {
                vt->current_attr.bg.mode  = COLOR_MODE_16;
                vt->current_attr.bg.index = v;
            } else if (v == 38 || v == 48) {
                // 256 or RGB color — parse from i
                ansi_color_t color = escape_decode_color(ep, &i);
                if (v == 38) vt->current_attr.fg = color;
                else         vt->current_attr.bg = color;
            }
            break;
        }
    }
}

// -------------------------------------------------------------------------
// cursor_move
// -------------------------------------------------------------------------
static void cursor_move(vt100_t *vt, int row, int col) {
    if (row < 0) row = 0;
    if (row >= vt->rows) row = vt->rows - 1;
    if (col < 0) col = 0;
    if (col >= vt->cols) col = vt->cols - 1;
    vt->cursor_row = row;
    vt->cursor_col = col;
}

// -------------------------------------------------------------------------
// screen_scroll_up
// -------------------------------------------------------------------------
static void screen_scroll_up(vt100_t *vt, int top, int bot, int n) {
    if (n <= 0 || top > bot) return;
    int height = bot - top + 1;
    if (n >= height) n = height;

    memmove(&vt->screen[top * vt->cols],
            &vt->screen[(top + n) * vt->cols],
            (size_t)((height - n) * vt->cols) * sizeof(vt100_cell_t));

    for (int r = bot - n + 1; r <= bot; r++) {
        for (int c = 0; c < vt->cols; c++) {
            vt100_cell_t *cell = &vt->screen[r * vt->cols + c];
            cell->ch = ' ';
            memset(&cell->attr, 0, sizeof(cell->attr));
        }
    }
}

// -------------------------------------------------------------------------
// screen_scroll_down
// -------------------------------------------------------------------------
static void screen_scroll_down(vt100_t *vt, int top, int bot, int n) {
    if (n <= 0 || top > bot) return;
    int height = bot - top + 1;
    if (n >= height) n = height;

    memmove(&vt->screen[(top + n) * vt->cols],
            &vt->screen[top * vt->cols],
            (size_t)((height - n) * vt->cols) * sizeof(vt100_cell_t));

    for (int r = top; r < top + n; r++) {
        for (int c = 0; c < vt->cols; c++) {
            vt100_cell_t *cell = &vt->screen[r * vt->cols + c];
            cell->ch = ' ';
            memset(&cell->attr, 0, sizeof(cell->attr));
        }
    }
}

// -------------------------------------------------------------------------
// erase_in_display
// -------------------------------------------------------------------------
static void erase_in_display(vt100_t *vt, int mode) {
    int start, end;
    int cursor = vt->cursor_row * vt->cols + vt->cursor_col;
    int total  = vt->rows * vt->cols;

    switch (mode) {
    case 0: start = cursor; end = total; break;     // from cursor to end
    case 1: start = 0;      end = cursor + 1; break; // from start to cursor
    case 2: start = 0;      end = total; break;      // entire screen
    case 3: start = 0;      end = total; break;      // entire screen + scrollback
    default: return;
    }

    for (int i = start; i < end; i++) {
        vt->screen[i].ch = ' ';
        memset(&vt->screen[i].attr, 0, sizeof(vt->screen[i].attr));
    }
}

// -------------------------------------------------------------------------
// erase_in_line
// -------------------------------------------------------------------------
static void erase_in_line(vt100_t *vt, int mode) {
    int row   = vt->cursor_row * vt->cols;
    int start, end;

    switch (mode) {
    case 0: start = vt->cursor_col; end = vt->cols; break;
    case 1: start = 0; end = vt->cursor_col + 1; break;
    case 2: start = 0; end = vt->cols; break;
    default: return;
    }

    for (int i = start; i < end; i++) {
        vt->screen[row + i].ch = ' ';
        memset(&vt->screen[row + i].attr, 0, sizeof(vt->screen[row + i].attr));
    }
}

// -------------------------------------------------------------------------
// vt100_resize
// -------------------------------------------------------------------------
int vt100_resize(vt100_t *vt, int cols, int rows) {
    if (!vt || cols <= 0 || rows <= 0) return -1;

    vt100_cell_t *new_screen = calloc((size_t)(rows * cols), sizeof(vt100_cell_t));
    if (!new_screen) return -1;

    // Copy what fits
    int copy_rows = rows < vt->rows ? rows : vt->rows;
    int copy_cols = cols < vt->cols ? cols : vt->cols;
    for (int r = 0; r < copy_rows; r++) {
        memcpy(&new_screen[r * cols],
               &vt->screen[r * vt->cols],
               (size_t)copy_cols * sizeof(vt100_cell_t));
    }

    free(vt->screen);
    vt->screen = new_screen;
    vt->cols   = cols;
    vt->rows   = rows;

    if (vt->cursor_row >= rows) vt->cursor_row = rows - 1;
    if (vt->cursor_col >= cols) vt->cursor_col = cols - 1;
    vt->scroll_top = 0;
    vt->scroll_bot = rows - 1;

    vt->dirty = 1;
    return 0;
}

// -------------------------------------------------------------------------
// vt100_get_cell
// -------------------------------------------------------------------------
const vt100_cell_t *vt100_get_cell(const vt100_t *vt, int row, int col) {
    if (!vt || row < 0 || row >= vt->rows || col < 0 || col >= vt->cols)
        return NULL;
    return &vt->screen[row * vt->cols + col];
}
