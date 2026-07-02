/*
 * VoidTerm - VT100 Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef VT100_H
#define VT100_H

#include <stddef.h>
#include "escape_parser.h"

typedef struct {
    ansi_color_t fg, bg;
    unsigned char bold      : 1;
    unsigned char underline : 1;
    unsigned char italic    : 1;
    unsigned char blink     : 1;
    unsigned char reverse   : 1;
} vt100_attr_t;

typedef struct {
    unsigned int  ch;     // Unicode codepoint
    vt100_attr_t  attr;
} vt100_cell_t;

typedef struct {
    int            cols, rows;
    int            cursor_row, cursor_col;
    int            saved_row,  saved_col;
    int            scroll_top, scroll_bot;
    vt100_cell_t  *screen;
    vt100_cell_t  *alt_screen;
    int            using_alt;
    vt100_attr_t   current_attr;
    escape_parser_t parser;
    int            dirty;
} vt100_t;

int                 vt100_init(vt100_t *vt, int cols, int rows);
void                vt100_destroy(vt100_t *vt);
void                vt100_feed(vt100_t *vt, const char *data, size_t len);
int                 vt100_resize(vt100_t *vt, int cols, int rows);
const vt100_cell_t *vt100_get_cell(const vt100_t *vt, int row, int col);

#endif /* VT100_H */
