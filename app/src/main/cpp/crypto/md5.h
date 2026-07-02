/*
 * VoidTerm - MD5 Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef MD5_H
#define MD5_H
#include <stdint.h>
#include <stddef.h>
typedef struct {
    uint32_t lo, hi, a, b, c, d;
    uint8_t  buffer[64];
} md5_ctx_t;
void md5_init(md5_ctx_t *ctx);
void md5_update(md5_ctx_t *ctx, const uint8_t *data, size_t len);
void md5_final(md5_ctx_t *ctx, uint8_t hash[16]);
void md5_hex(const uint8_t hash[16], char out[33]);
#endif
