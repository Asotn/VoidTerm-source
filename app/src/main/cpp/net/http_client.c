/*
 * VoidTerm - HTTP Client
 * Minimal HTTP/HTTPS client for package downloads.
 * Uses POSIX sockets for HTTP; HTTPS requires openssl in the proot env.
 * For the bootstrapper itself, we use Java-side HttpURLConnection.
 * This C layer provides synchronous download helpers for post-bootstrap use.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

#include "http_client.h"
#include "../shell/shell_quote.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <fcntl.h>
#include <android/log.h>

#define LOG_TAG "VoidTerm-HTTP"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#define HTTP_READ_BUF_SIZE (64 * 1024)
#define HTTP_MAX_HEADER    (8 * 1024)

// -------------------------------------------------------------------------
// Internal: parse URL
// -------------------------------------------------------------------------
typedef struct {
    char host[256];
    char path[1024];
    int  port;
    int  is_https;
} parsed_url_t;

static int parse_url(const char *url, parsed_url_t *out) {
    if (!url || !out) return -1;
    memset(out, 0, sizeof(*out));

    if (strncmp(url, "https://", 8) == 0) {
        out->is_https = 1;
        url += 8;
        out->port = 443;
    } else if (strncmp(url, "http://", 7) == 0) {
        out->is_https = 0;
        url += 7;
        out->port = 80;
    } else {
        return -1;
    }

    const char *slash = strchr(url, '/');
    if (slash) {
        size_t host_len = (size_t)(slash - url);
        if (host_len >= sizeof(out->host)) return -1;
        strncpy(out->host, url, host_len);
        strncpy(out->path, slash, sizeof(out->path) - 1);
    } else {
        strncpy(out->host, url, sizeof(out->host) - 1);
        strcpy(out->path, "/");
    }

    // Check for port in host
    char *colon = strchr(out->host, ':');
    if (colon) {
        out->port = atoi(colon + 1);
        *colon = '\0';
    }

    return 0;
}

// -------------------------------------------------------------------------
// Internal: connect TCP
// -------------------------------------------------------------------------
static int tcp_connect(const char *host, int port) {
    struct addrinfo hints, *res;
    char port_str[16];
    snprintf(port_str, sizeof(port_str), "%d", port);

    memset(&hints, 0, sizeof(hints));
    hints.ai_family   = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;

    int err = getaddrinfo(host, port_str, &hints, &res);
    if (err != 0) {
        LOGE("getaddrinfo(%s:%d): %s", host, port, gai_strerror(err));
        return -1;
    }

    int sock = -1;
    for (struct addrinfo *rp = res; rp; rp = rp->ai_next) {
        sock = socket(rp->ai_family, rp->ai_socktype, rp->ai_protocol);
        if (sock < 0) continue;
        if (connect(sock, rp->ai_addr, rp->ai_addrlen) == 0) break;
        close(sock);
        sock = -1;
    }
    freeaddrinfo(res);

    if (sock < 0) LOGE("tcp_connect(%s:%d) failed", host, port);
    return sock;
}

// -------------------------------------------------------------------------
// http_download
// Download a URL to a file path.
// progress_fn is called periodically with (bytes_received, total_bytes, userdata).
// Returns 0 on success.
// -------------------------------------------------------------------------
int http_download(const char *url, const char *dest_path,
                   http_progress_fn progress_fn, void *progress_userdata) {
    if (!url || !dest_path) return -1;

    // HTTPS: use curl inside proot for now (post-bootstrap)
    if (strncmp(url, "https://", 8) == 0) {
        char q_url[2200], q_dest[1200], cmd[4096];
        if (shell_quote(url, q_url, sizeof(q_url)) != 0) {
            LOGE("http_download: URL too long/unsafe to quote");
            return -1;
        }
        if (shell_quote(dest_path, q_dest, sizeof(q_dest)) != 0) {
            LOGE("http_download: dest_path too long/unsafe to quote");
            return -1;
        }
        // --proto/--tlsv1.2/fail-fast pin curl to HTTPS with modern TLS and
        // reject redirects to non-HTTPS URLs; certificate validation is on
        // by default (no -k/--insecure is ever passed).
        snprintf(cmd, sizeof(cmd),
            "curl -fsSL --proto '=https' --tlsv1.2 --progress-bar -o %s %s",
            q_dest, q_url);
        return system(cmd);
    }

    parsed_url_t purl;
    if (parse_url(url, &purl) != 0) {
        LOGE("Failed to parse URL: %s", url);
        return -1;
    }

    int sock = tcp_connect(purl.host, purl.port);
    if (sock < 0) return -1;

    // Build HTTP request
    char req[2048];
    int reqlen = snprintf(req, sizeof(req),
        "GET %s HTTP/1.1\r\n"
        "Host: %s\r\n"
        "User-Agent: VoidTerm/1.0\r\n"
        "Accept: */*\r\n"
        "Connection: close\r\n"
        "\r\n",
        purl.path, purl.host);

    if (send(sock, req, (size_t)reqlen, 0) < 0) {
        LOGE("send failed: %s", strerror(errno));
        close(sock);
        return -1;
    }

    // Read response headers
    char header_buf[HTTP_MAX_HEADER];
    int  header_len = 0;
    char c;
    while (header_len < HTTP_MAX_HEADER - 1) {
        ssize_t n = recv(sock, &c, 1, 0);
        if (n <= 0) break;
        header_buf[header_len++] = c;
        if (header_len >= 4 &&
            header_buf[header_len-4] == '\r' && header_buf[header_len-3] == '\n' &&
            header_buf[header_len-2] == '\r' && header_buf[header_len-1] == '\n') {
            break;
        }
    }
    header_buf[header_len] = '\0';

    // Parse status code
    int status_code = 0;
    sscanf(header_buf, "HTTP/%*d.%*d %d", &status_code);
    if (status_code != 200) {
        LOGE("HTTP %d for %s", status_code, url);
        close(sock);
        return -1;
    }

    // Parse Content-Length
    long content_length = -1;
    char *cl = strcasestr(header_buf, "Content-Length:");
    if (cl) sscanf(cl + 15, " %ld", &content_length);

    // Open dest file
    FILE *f = fopen(dest_path, "wb");
    if (!f) {
        LOGE("fopen(%s): %s", dest_path, strerror(errno));
        close(sock);
        return -1;
    }

    // Read body
    char *buf = malloc(HTTP_READ_BUF_SIZE);
    if (!buf) { fclose(f); close(sock); return -1; }

    long downloaded = 0;
    ssize_t n;
    while ((n = recv(sock, buf, HTTP_READ_BUF_SIZE, 0)) > 0) {
        fwrite(buf, 1, (size_t)n, f);
        downloaded += n;

        if (progress_fn && content_length > 0) {
            int pct = (int)((downloaded * 100) / content_length);
            progress_fn(downloaded, content_length, pct, progress_userdata);
        }
    }

    free(buf);
    fclose(f);
    close(sock);
    LOGI("Downloaded %ld bytes to %s", downloaded, dest_path);
    return 0;
}

// -------------------------------------------------------------------------
// http_fetch_string
// Fetch a URL into a string buffer (for APT InRelease, Packages, etc.)
// -------------------------------------------------------------------------
int http_fetch_string(const char *url, char *buf, size_t buf_size) {
    if (!url || !buf || buf_size == 0) return -1;

    // Use curl in proot for HTTPS
    if (strncmp(url, "https://", 8) == 0) {
        char q_url[2200], cmd[2400];
        if (shell_quote(url, q_url, sizeof(q_url)) != 0) {
            LOGE("http_fetch_string: URL too long/unsafe to quote");
            return -1;
        }
        snprintf(cmd, sizeof(cmd),
            "curl -fsSL --proto '=https' --tlsv1.2 %s", q_url);
        FILE *p = popen(cmd, "r");
        if (!p) return -1;
        size_t total = 0;
        char tmp[4096];
        size_t n;
        while ((n = fread(tmp, 1, sizeof(tmp), p)) > 0 && total + n < buf_size - 1) {
            memcpy(buf + total, tmp, n);
            total += n;
        }
        buf[total] = '\0';
        pclose(p);
        return 0;
    }

    parsed_url_t purl;
    if (parse_url(url, &purl) != 0) return -1;

    int sock = tcp_connect(purl.host, purl.port);
    if (sock < 0) return -1;

    char req[2048];
    int reqlen = snprintf(req, sizeof(req),
        "GET %s HTTP/1.0\r\nHost: %s\r\nUser-Agent: VoidTerm/1.0\r\n\r\n",
        purl.path, purl.host);
    send(sock, req, (size_t)reqlen, 0);

    size_t total = 0;
    char tmp[4096];
    ssize_t n;
    int headers_done = 0;
    char header_acc[HTTP_MAX_HEADER];
    int  hdr_len = 0;

    while ((n = recv(sock, tmp, sizeof(tmp), 0)) > 0) {
        if (!headers_done) {
            for (ssize_t i = 0; i < n; i++) {
                header_acc[hdr_len++] = tmp[i];
                if (hdr_len >= 4 &&
                    header_acc[hdr_len-4] == '\r' && header_acc[hdr_len-3] == '\n' &&
                    header_acc[hdr_len-2] == '\r' && header_acc[hdr_len-1] == '\n') {
                    headers_done = 1;
                    ssize_t remaining = n - i - 1;
                    if (remaining > 0 && total + (size_t)remaining < buf_size - 1) {
                        memcpy(buf + total, tmp + i + 1, (size_t)remaining);
                        total += (size_t)remaining;
                    }
                    break;
                }
            }
        } else {
            if (total + (size_t)n < buf_size - 1) {
                memcpy(buf + total, tmp, (size_t)n);
                total += (size_t)n;
            }
        }
    }
    if (total < buf_size) buf[total] = '\0';
    close(sock);
    return 0;
}
