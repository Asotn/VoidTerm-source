/*
 * VoidTerm - SHA-256 Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */

#ifndef SHA256_H
#define SHA256_H

#ifdef __cplusplus
extern "C" {
#endif


#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <string.h>

typedef struct {
    uint8_t  data[64];
    uint32_t datalen;
    uint64_t bitlen;
    uint32_t state[8];
} sha256_ctx_t;

void sha256_init(sha256_ctx_t *ctx);
void sha256_update(sha256_ctx_t *ctx, const uint8_t *data, size_t len);
void sha256_final(sha256_ctx_t *ctx, uint8_t hash[32]);
void sha256_hex(const uint8_t hash[32], char out[65]);
int  sha256_file(const char *path, char out_hex[65]);
int  sha256_verify_file(const char *path, const char *expected_hex);


#ifdef __cplusplus
}
#endif

#endif /* SHA256_H */
