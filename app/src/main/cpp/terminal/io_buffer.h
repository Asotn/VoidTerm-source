/*
 * VoidTerm - I/O Buffer Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef IO_BUFFER_H
#define IO_BUFFER_H

#include <stddef.h>
#include <stdatomic.h>

typedef struct {
    char          *data;
    size_t         capacity;
    size_t         mask;
    _Atomic size_t head;
    _Atomic size_t tail;
} io_buffer_t;

int    io_buffer_init(io_buffer_t *b, size_t capacity);
void   io_buffer_destroy(io_buffer_t *b);
size_t io_buffer_write(io_buffer_t *b, const char *src, size_t len);
size_t io_buffer_read(io_buffer_t *b, char *dst, size_t len);
size_t io_buffer_peek(const io_buffer_t *b, char *dst, size_t len);
size_t io_buffer_available(const io_buffer_t *b);
size_t io_buffer_free_space(const io_buffer_t *b);
void   io_buffer_clear(io_buffer_t *b);
int    io_buffer_is_empty(const io_buffer_t *b);

#endif /* IO_BUFFER_H */
