/*
 * VoidTerm - DistroCatalog
 * Numbered list of installable Linux distributions. URLs point to each
 * distro project's OWN official rootfs archives (Ubuntu Base, Debian's
 * official Docker rootfs mirror, Kali's own NetHunter rootfs, Arch Linux
 * ARM, and Alpine) — nothing is repackaged or modified by VoidTerm.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
package com.asotn.voidterm.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DistroCatalog {

    public static final class Distro {
        public final int number;
        public final String id;          // short id, e.g. "kali"
        public final String displayName; // shown to the user
        /** archKey (arm64 / armhf / amd64 / i386) -> rootfs download URL */
        public final Map<String, String> urlsByArch;
        /** true if the archive extracts into a single top-level folder that
         *  must be stripped (Ubuntu Base / Alpine tarballs extract flat, but
         *  Arch Linux ARM also extracts flat — kept here for future distros
         *  that don't). */
        public final boolean flatExtract;

        Distro(int number, String id, String displayName,
               Map<String, String> urlsByArch, boolean flatExtract) {
            this.number = number;
            this.id = id;
            this.displayName = displayName;
            this.urlsByArch = urlsByArch;
            this.flatExtract = flatExtract;
        }

        public String urlFor(String archKey) {
            String u = urlsByArch.get(archKey);
            return u != null ? u : urlsByArch.get("arm64"); // best-effort fallback
        }
    }

    private static final List<Distro> DISTROS = new ArrayList<>();
    static {
        DISTROS.add(new Distro(1, "kali", "Kali Linux",
                map("arm64", "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz",
                    "armhf", "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-armhf.tar.xz"),
                true));

        DISTROS.add(new Distro(2, "ubuntu", "Ubuntu 22.04 (Jammy)",
                map("arm64", "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.4-base-arm64.tar.gz",
                    "armhf", "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.4-base-armhf.tar.gz"),
                true));

        DISTROS.add(new Distro(3, "debian", "Debian 12 (Bookworm)",
                map("arm64", "https://github.com/debuerreotype/docker-debian-artifacts/raw/bookworm/arm64v8/rootfs.tar.xz",
                    "armhf", "https://github.com/debuerreotype/docker-debian-artifacts/raw/bookworm/arm32v7/rootfs.tar.xz"),
                true));

        DISTROS.add(new Distro(4, "archlinux", "Arch Linux ARM",
                map("arm64", "https://ca.us.mirror.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz",
                    "armhf", "https://ca.us.mirror.archlinuxarm.org/os/ArchLinuxARM-armv7-latest.tar.gz"),
                true));

        DISTROS.add(new Distro(5, "alpine", "Alpine Linux 3.19 (lightweight)",
                map("arm64", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-minirootfs-3.19.1-aarch64.tar.gz",
                    "armhf", "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/armhf/alpine-minirootfs-3.19.1-armhf.tar.gz"),
                true));
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private DistroCatalog() { }

    public static List<Distro> all() {
        return DISTROS;
    }

    public static Distro byNumber(int n) {
        for (Distro d : DISTROS) if (d.number == n) return d;
        return null;
    }

    /** Renders the numbered menu shown to the user on first launch. */
    public static String renderMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("VoidTerm needs a Linux environment to run real Linux tools.\n");
        sb.append("Choose a distro to install (type the number and press Enter):\n\n");
        for (Distro d : DISTROS) {
            sb.append("  ").append(d.number).append(") ").append(d.displayName).append('\n');
        }
        sb.append("\n> ");
        return sb.toString();
    }
}
