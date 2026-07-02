/*
 * VoidTerm - I/O Ring Buffer
 * Lock-free ring buffer for PTY read/write buffering.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "io_buffer.h"
#include <string.h>
#include <stdlib.h>
#include <stdatomic.h>

// -------------------------------------------------------------------------
// io_buffer_init
// -------------------------------------------------------------------------
int io_buffer_init(io_buffer_t *b, size_t capacity) {
    if (!b || capacity == 0) return -1;

    // Round up to power of 2
    size_t cap = 1;
    while (cap < capacity) cap <<= 1;

    b->data = malloc(cap);
    if (!b->data) return -1;

    b->capacity = cap;
    b->mask     = cap - 1;
    atomic_store(&b->head, 0);
    atomic_store(&b->tail, 0);
    return 0;
}

// -------------------------------------------------------------------------
// io_buffer_destroy
// -------------------------------------------------------------------------
void io_buffer_destroy(io_buffer_t *b) {
    if (!b) return;
    free(b->data);
    b->data     = NULL;
    b->capacity = 0;
}

// -------------------------------------------------------------------------
// io_buffer_write
// Returns number of bytes written.
// -------------------------------------------------------------------------
size_t io_buffer_write(io_buffer_t *b, const char *src, size_t len) {
    if (!b || !src || len == 0) return 0;

    size_t head = atomic_load_explicit(&b->head, memory_order_relaxed);
    size_t tail = atomic_load_explicit(&b->tail, memory_order_acquire);
    size_t avail = b->capacity - (head - tail);

    if (len > avail) len = avail;
    if (len == 0) return 0;

    size_t pos    = head & b->mask;
    size_t first  = b->capacity - pos;

    if (first >= len) {
        memcpy(b->data + pos, src, len);
    } else {
        memcpy(b->data + pos, src, first);
        memcpy(b->data, src + first, len - first);
    }

    atomic_store_explicit(&b->head, head + len, memory_order_release);
    return len;
}

// -------------------------------------------------------------------------
// io_buffer_read
// Returns number of bytes read.
// -------------------------------------------------------------------------
size_t io_buffer_read(io_buffer_t *b, char *dst, size_t len) {
    if (!b || !dst || len == 0) return 0;

    size_t tail = atomic_load_explicit(&b->tail, memory_order_relaxed);
    size_t head = atomic_load_explicit(&b->head, memory_order_acquire);
    size_t avail = head - tail;

    if (len > avail) len = avail;
    if (len == 0) return 0;

    size_t pos   = tail & b->mask;
    size_t first = b->capacity - pos;

    if (first >= len) {
        memcpy(dst, b->data + pos, len);
    } else {
        memcpy(dst, b->data + pos, first);
        memcpy(dst + first, b->data, len - first);
    }

    atomic_store_explicit(&b->tail, tail + len, memory_order_release);
    return len;
}

// -------------------------------------------------------------------------
// io_buffer_peek
// Read without consuming.
// -------------------------------------------------------------------------
size_t io_buffer_peek(const io_buffer_t *b, char *dst, size_t len) {
    if (!b || !dst || len == 0) return 0;

    size_t tail  = atomic_load_explicit(&b->tail, memory_order_relaxed);
    size_t head  = atomic_load_explicit(&b->head, memory_order_acquire);
    size_t avail = head - tail;

    if (len > avail) len = avail;
    if (len == 0) return 0;

    size_t pos   = tail & b->mask;
    size_t first = b->capacity - pos;

    if (first >= len) {
        memcpy(dst, b->data + pos, len);
    } else {
        memcpy(dst, b->data + pos, first);
        memcpy(dst + first, b->data, len - first);
    }

    return len;
}

// -------------------------------------------------------------------------
// io_buffer_available
// -------------------------------------------------------------------------
size_t io_buffer_available(const io_buffer_t *b) {
    if (!b) return 0;
    size_t h = atomic_load_explicit(&b->head, memory_order_acquire);
    size_t t = atomic_load_explicit(&b->tail, memory_order_acquire);
    return h - t;
}

// -------------------------------------------------------------------------
// io_buffer_free_space
// -------------------------------------------------------------------------
size_t io_buffer_free_space(const io_buffer_t *b) {
    if (!b) return 0;
    return b->capacity - io_buffer_available(b);
}

// -------------------------------------------------------------------------
// io_buffer_clear
// -------------------------------------------------------------------------
void io_buffer_clear(io_buffer_t *b) {
    if (!b) return;
    size_t h = atomic_load_explicit(&b->head, memory_order_acquire);
    atomic_store_explicit(&b->tail, h, memory_order_release);
}

// -------------------------------------------------------------------------
// io_buffer_is_empty
// -------------------------------------------------------------------------
int io_buffer_is_empty(const io_buffer_t *b) {
    return io_buffer_available(b) == 0;
}
