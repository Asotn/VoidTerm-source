/*
 * VoidTerm - MD5 Implementation
 * Used for legacy APT checksum verification (some older repos use MD5).
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "md5.h"
#include <string.h>
#include <stdint.h>
#include <stdio.h>

#define F(x,y,z) (((x) & (y)) | (~(x) & (z)))
#define G(x,y,z) (((x) & (z)) | ((y) & ~(z)))
#define H(x,y,z) ((x) ^ (y) ^ (z))
#define I(x,y,z) ((y) ^ ((x) | ~(z)))
#define ROTL(x,n) (((x) << (n)) | ((x) >> (32-(n))))

static const uint32_t T[64] = {
    0xd76aa478,0xe8c7b756,0x242070db,0xc1bdceee,
    0xf57c0faf,0x4787c62a,0xa8304613,0xfd469501,
    0x698098d8,0x8b44f7af,0xffff5bb1,0x895cd7be,
    0x6b901122,0xfd987193,0xa679438e,0x49b40821,
    0xf61e2562,0xc040b340,0x265e5a51,0xe9b6c7aa,
    0xd62f105d,0x02441453,0xd8a1e681,0xe7d3fbc8,
    0x21e1cde6,0xc33707d6,0xf4d50d87,0x455a14ed,
    0xa9e3e905,0xfcefa3f8,0x676f02d9,0x8d2a4c8a,
    0xfffa3942,0x8771f681,0x6d9d6122,0xfde5380c,
    0xa4beea44,0x4bdecfa9,0xf6bb4b60,0xbebfbc70,
    0x289b7ec6,0xeaa127fa,0xd4ef3085,0x04881d05,
    0xd9d4d039,0xe6db99e5,0x1fa27cf8,0xc4ac5665,
    0xf4292244,0x432aff97,0xab9423a7,0xfc93a039,
    0x655b59c3,0x8f0ccc92,0xffeff47d,0x85845dd1,
    0x6fa87e4f,0xfe2ce6e0,0xa3014314,0x4e0811a1,
    0xf7537e82,0xbd3af235,0x2ad7d2bb,0xeb86d391
};

static const int S[64] = {
    7,12,17,22, 7,12,17,22, 7,12,17,22, 7,12,17,22,
    5, 9,14,20, 5, 9,14,20, 5, 9,14,20, 5, 9,14,20,
    4,11,16,23, 4,11,16,23, 4,11,16,23, 4,11,16,23,
    6,10,15,21, 6,10,15,21, 6,10,15,21, 6,10,15,21
};

void md5_init(md5_ctx_t *ctx) {
    ctx->lo = ctx->hi = 0;
    ctx->a = 0x67452301;
    ctx->b = 0xefcdab89;
    ctx->c = 0x98badcfe;
    ctx->d = 0x10325476;
}

static void md5_transform(md5_ctx_t *ctx, const uint8_t *block) {
    uint32_t a = ctx->a, b = ctx->b, c = ctx->c, d = ctx->d;
    uint32_t x[16];
    for (int i = 0; i < 16; i++) {
        x[i] = (uint32_t)block[i*4]
             | ((uint32_t)block[i*4+1] <<  8)
             | ((uint32_t)block[i*4+2] << 16)
             | ((uint32_t)block[i*4+3] << 24);
    }

    for (int i = 0; i < 64; i++) {
        uint32_t fn, g;
        if (i < 16) {
            fn = F(b,c,d); g = (uint32_t)i;
        } else if (i < 32) {
            fn = G(b,c,d); g = (5*(uint32_t)i + 1) % 16;
        } else if (i < 48) {
            fn = H(b,c,d); g = (3*(uint32_t)i + 5) % 16;
        } else {
            fn = I(b,c,d); g = (7*(uint32_t)i) % 16;
        }
        fn += a + T[i] + x[g];
        a = d; d = c; c = b;
        b += ROTL(fn, S[i]);
    }
    ctx->a += a; ctx->b += b; ctx->c += c; ctx->d += d;
}

void md5_update(md5_ctx_t *ctx, const uint8_t *data, size_t len) {
    uint32_t used = ctx->lo & 0x3f;
    ctx->lo += (uint32_t)(len * 8);
    if (ctx->lo < (uint32_t)(len * 8)) ctx->hi++;
    ctx->hi += (uint32_t)(len >> 29);

    if (used) {
        uint32_t avail = 64 - used;
        if (len < avail) {
            memcpy(&ctx->buffer[used], data, len);
            return;
        }
        memcpy(&ctx->buffer[used], data, avail);
        md5_transform(ctx, ctx->buffer);
        data += avail; len -= avail;
    }
    while (len >= 64) {
        md5_transform(ctx, data);
        data += 64; len -= 64;
    }
    memcpy(ctx->buffer, data, len);
}

void md5_final(md5_ctx_t *ctx, uint8_t hash[16]) {
    uint32_t used = ctx->lo & 0x3f;
    ctx->buffer[used++] = 0x80;
    if (used > 56) {
        memset(&ctx->buffer[used], 0, 64 - used);
        md5_transform(ctx, ctx->buffer);
        used = 0;
    }
    memset(&ctx->buffer[used], 0, 56 - used);
    ctx->buffer[56] = (uint8_t)(ctx->lo);
    ctx->buffer[57] = (uint8_t)(ctx->lo >> 8);
    ctx->buffer[58] = (uint8_t)(ctx->lo >> 16);
    ctx->buffer[59] = (uint8_t)(ctx->lo >> 24);
    ctx->buffer[60] = (uint8_t)(ctx->hi);
    ctx->buffer[61] = (uint8_t)(ctx->hi >> 8);
    ctx->buffer[62] = (uint8_t)(ctx->hi >> 16);
    ctx->buffer[63] = (uint8_t)(ctx->hi >> 24);
    md5_transform(ctx, ctx->buffer);
    hash[0]  = (uint8_t)(ctx->a);       hash[1]  = (uint8_t)(ctx->a >> 8);
    hash[2]  = (uint8_t)(ctx->a >> 16); hash[3]  = (uint8_t)(ctx->a >> 24);
    hash[4]  = (uint8_t)(ctx->b);       hash[5]  = (uint8_t)(ctx->b >> 8);
    hash[6]  = (uint8_t)(ctx->b >> 16); hash[7]  = (uint8_t)(ctx->b >> 24);
    hash[8]  = (uint8_t)(ctx->c);       hash[9]  = (uint8_t)(ctx->c >> 8);
    hash[10] = (uint8_t)(ctx->c >> 16); hash[11] = (uint8_t)(ctx->c >> 24);
    hash[12] = (uint8_t)(ctx->d);       hash[13] = (uint8_t)(ctx->d >> 8);
    hash[14] = (uint8_t)(ctx->d >> 16); hash[15] = (uint8_t)(ctx->d >> 24);
}

void md5_hex(const uint8_t hash[16], char out[33]) {
    static const char hex[] = "0123456789abcdef";
    for (int i = 0; i < 16; i++) {
        out[i*2]   = hex[(hash[i] >> 4) & 0xf];
        out[i*2+1] = hex[hash[i] & 0xf];
    }
    out[32] = '\0';
}
