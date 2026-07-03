/*
 * VoidTerm - PGP Verify Header
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 */
#ifndef PGP_VERIFY_H
#define PGP_VERIFY_H

#ifdef __cplusplus
extern "C" {
#endif

int pgp_verify_release(const char *proot_bin, const char *rootfs, const char *inrelease_path);
int pgp_import_kali_key(const char *proot_bin, const char *rootfs);
int pgp_check_available(const char *proot_bin, const char *rootfs);

#ifdef __cplusplus
}
#endif

#endif
