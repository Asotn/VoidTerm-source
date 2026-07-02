/*
 * VoidTerm - KaliTools Catalog
 * A searchable catalog of popular Kali Linux tools with categories,
 * descriptions, and apt install commands.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KaliTools {

    public enum Category {
        INFORMATION_GATHERING,
        VULNERABILITY_ANALYSIS,
        WEB_APPLICATION,
        DATABASE,
        PASSWORD_ATTACKS,
        WIRELESS,
        REVERSE_ENGINEERING,
        EXPLOITATION,
        SNIFFING_SPOOFING,
        POST_EXPLOITATION,
        FORENSICS,
        REPORTING,
        SOCIAL_ENGINEERING,
        ANDROID,
        CRYPTO
    }

    public static class Tool {
        public final String   name;
        public final String   pkg;
        public final String   description;
        public final Category category;

        public Tool(String name, String pkg, String description, Category category) {
            this.name        = name;
            this.pkg         = pkg;
            this.description = description;
            this.category    = category;
        }

        public String getInstallCommand() {
            return "apt-get install -y " + pkg;
        }
    }

    // -------------------------------------------------------------------------
    // Full tool catalog
    // -------------------------------------------------------------------------
    private static final List<Tool> TOOLS = Arrays.asList(

        // ---- Information Gathering ----
        new Tool("Nmap",         "nmap",          "Network mapper and port scanner",                       Category.INFORMATION_GATHERING),
        new Tool("Masscan",      "masscan",        "Mass IP port scanner (fast)",                          Category.INFORMATION_GATHERING),
        new Tool("theHarvester", "theharvester",   "Email, domain, host OSINT gatherer",                   Category.INFORMATION_GATHERING),
        new Tool("Recon-ng",     "recon-ng",       "Full-featured web reconnaissance framework",           Category.INFORMATION_GATHERING),
        new Tool("Maltego",      "maltego",        "Visual link analysis for OSINT",                       Category.INFORMATION_GATHERING),
        new Tool("Shodan",       "python3-shodan", "Shodan API client for device search",                  Category.INFORMATION_GATHERING),
        new Tool("DNSrecon",     "dnsrecon",       "DNS enumeration and zone transfer tool",               Category.INFORMATION_GATHERING),
        new Tool("DNSenum",      "dnsenum",        "DNS enumeration tool",                                 Category.INFORMATION_GATHERING),
        new Tool("Fierce",       "fierce",         "DNS reconnaissance tool",                              Category.INFORMATION_GATHERING),
        new Tool("Whois",        "whois",          "Domain WHOIS lookup tool",                             Category.INFORMATION_GATHERING),
        new Tool("Netdiscover",  "netdiscover",    "ARP network scanner",                                  Category.INFORMATION_GATHERING),
        new Tool("Amass",        "amass",          "In-depth attack surface mapping",                      Category.INFORMATION_GATHERING),
        new Tool("Subfinder",    "subfinder",      "Fast passive subdomain enumeration",                   Category.INFORMATION_GATHERING),
        new Tool("Fping",        "fping",          "Fast host sweeper using ICMP",                         Category.INFORMATION_GATHERING),
        new Tool("Traceroute",   "traceroute",     "Network path tracing tool",                            Category.INFORMATION_GATHERING),
        new Tool("SNMP-check",   "snmpcheck",      "SNMP device enumeration",                              Category.INFORMATION_GATHERING),
        new Tool("onesixtyone",  "onesixtyone",    "Fast SNMP scanner",                                    Category.INFORMATION_GATHERING),
        new Tool("Enum4linux",   "enum4linux",     "Enumerate information from Windows/Samba systems",     Category.INFORMATION_GATHERING),
        new Tool("NBTscan",      "nbtscan",        "NetBIOS name scanner",                                 Category.INFORMATION_GATHERING),
        new Tool("p0f",          "p0f",            "Passive OS fingerprinting tool",                       Category.INFORMATION_GATHERING),
        new Tool("hping3",       "hping3",         "Active network security tool",                         Category.INFORMATION_GATHERING),
        new Tool("Zenmap",       "zenmap",         "Nmap GUI frontend",                                    Category.INFORMATION_GATHERING),
        new Tool("Dmitry",       "dmitry",         "Deepmagic information gathering tool",                 Category.INFORMATION_GATHERING),
        new Tool("Spiderfoot",   "spiderfoot",     "Automated OSINT collection tool",                      Category.INFORMATION_GATHERING),

        // ---- Vulnerability Analysis ----
        new Tool("Nikto",        "nikto",          "Web server vulnerability scanner",                     Category.VULNERABILITY_ANALYSIS),
        new Tool("OpenVAS",      "openvas",        "Full-featured vulnerability scanner",                  Category.VULNERABILITY_ANALYSIS),
        new Tool("Lynis",        "lynis",          "Unix/Linux security auditing tool",                    Category.VULNERABILITY_ANALYSIS),
        new Tool("Nessus",       "nessus",         "Comprehensive vulnerability scanner",                  Category.VULNERABILITY_ANALYSIS),
        new Tool("Vulnix",       "vulnix",         "NixOS vulnerability scanner",                          Category.VULNERABILITY_ANALYSIS),
        new Tool("Unix-privesc-check", "unix-privesc-check", "Privilege escalation checker",              Category.VULNERABILITY_ANALYSIS),

        // ---- Web Application ----
        new Tool("Burp Suite",   "burpsuite",      "Web application security testing platform",            Category.WEB_APPLICATION),
        new Tool("OWASP ZAP",    "zaproxy",        "Web application security scanner",                     Category.WEB_APPLICATION),
        new Tool("SQLmap",       "sqlmap",         "Automatic SQL injection and database takeover",        Category.WEB_APPLICATION),
        new Tool("Dirb",         "dirb",           "URL brute-force scanner",                              Category.WEB_APPLICATION),
        new Tool("Gobuster",     "gobuster",       "Directory/file/DNS/VHost brute forcer",                Category.WEB_APPLICATION),
        new Tool("Ffuf",         "ffuf",           "Fast web fuzzer written in Go",                        Category.WEB_APPLICATION),
        new Tool("Wfuzz",        "wfuzz",          "Web application fuzzer",                               Category.WEB_APPLICATION),
        new Tool("WPScan",       "wpscan",         "WordPress vulnerability scanner",                      Category.WEB_APPLICATION),
        new Tool("XSStrike",     "xsstrike",       "Advanced XSS detection suite",                         Category.WEB_APPLICATION),
        new Tool("Commix",       "commix",         "Automated command injection tool",                     Category.WEB_APPLICATION),
        new Tool("Skipfish",     "skipfish",       "Active web application security reconnaissance",       Category.WEB_APPLICATION),
        new Tool("Arachni",      "arachni",        "Web application security scanner framework",           Category.WEB_APPLICATION),
        new Tool("Feroxbuster",  "feroxbuster",    "Fast, recursive content discovery tool",               Category.WEB_APPLICATION),
        new Tool("HTTPx",        "httpx",          "Fast HTTP toolkit for probing",                        Category.WEB_APPLICATION),
        new Tool("Nuclei",       "nuclei",         "Template-based vulnerability scanner",                  Category.WEB_APPLICATION),

        // ---- Password Attacks ----
        new Tool("John",         "john",           "John the Ripper password cracker",                     Category.PASSWORD_ATTACKS),
        new Tool("Hashcat",      "hashcat",        "Advanced password recovery utility",                   Category.PASSWORD_ATTACKS),
        new Tool("Hydra",        "hydra",          "Online password brute-force tool",                     Category.PASSWORD_ATTACKS),
        new Tool("Medusa",       "medusa",         "Parallel network login auditor",                       Category.PASSWORD_ATTACKS),
        new Tool("Ncrack",       "ncrack",         "High-speed network authentication cracker",            Category.PASSWORD_ATTACKS),
        new Tool("CrackMapExec", "crackmapexec",   "Swiss army knife for pentesting networks",             Category.PASSWORD_ATTACKS),
        new Tool("Cewl",         "cewl",           "Custom wordlist generator from web pages",             Category.PASSWORD_ATTACKS),
        new Tool("Crunch",       "crunch",         "Wordlist generator",                                   Category.PASSWORD_ATTACKS),
        new Tool("Ophcrack",     "ophcrack",       "Windows password cracker using rainbow tables",        Category.PASSWORD_ATTACKS),
        new Tool("Hash-Identifier","hash-identifier","Identifies different types of hashes",               Category.PASSWORD_ATTACKS),
        new Tool("Hashid",       "hashid",         "Hash type identifier",                                 Category.PASSWORD_ATTACKS),

        // ---- Wireless ----
        new Tool("Aircrack-ng",  "aircrack-ng",    "Complete suite for WiFi security auditing",            Category.WIRELESS),
        new Tool("Kismet",       "kismet",         "Wireless network detector and sniffer",                Category.WIRELESS),
        new Tool("Reaver",       "reaver",         "WPS attack tool",                                      Category.WIRELESS),
        new Tool("Bully",        "bully",          "WPS brute force attack tool",                          Category.WIRELESS),
        new Tool("Pixiewps",     "pixiewps",       "Offline WPS PIN cracker",                              Category.WIRELESS),
        new Tool("Fern WiFi Cracker","fern-wifi-cracker","GUI wireless security auditing tool",            Category.WIRELESS),
        new Tool("Wifite",       "wifite",         "Automated wireless attack tool",                       Category.WIRELESS),
        new Tool("MDK4",         "mdk4",           "IEEE 802.11 DoS tool",                                 Category.WIRELESS),
        new Tool("Hostapd-wpe",  "hostapd-wpe",   "Wireless evil AP tool",                                Category.WIRELESS),
        new Tool("Fluxion",      "fluxion",        "MITM WPA attack framework",                            Category.WIRELESS),
        new Tool("Bluetooth tools","bluetooth",    "Bluetooth protocol tools",                             Category.WIRELESS),

        // ---- Exploitation ----
        new Tool("Metasploit",   "metasploit-framework","Penetration testing framework",                   Category.EXPLOITATION),
        new Tool("Msfvenom",     "metasploit-framework","Payload generator and encoder",                   Category.EXPLOITATION),
        new Tool("BeEF",         "beef-xss",       "Browser exploitation framework",                       Category.EXPLOITATION),
        new Tool("Searchsploit", "exploitdb",      "Offline copy of exploit-db",                           Category.EXPLOITATION),
        new Tool("RouterSploit", "routersploit",   "Router exploitation framework",                        Category.EXPLOITATION),
        new Tool("Pwncat",       "pwncat",         "Reverse shell handler",                                Category.EXPLOITATION),
        new Tool("Shellter",     "shellter",       "Dynamic shellcode injection tool",                     Category.EXPLOITATION),
        new Tool("Veil",         "veil",           "AV-evasion tool for Metasploit payloads",              Category.EXPLOITATION),

        // ---- Sniffing & Spoofing ----
        new Tool("Wireshark",    "wireshark",      "Network protocol analyzer",                            Category.SNIFFING_SPOOFING),
        new Tool("Tshark",       "tshark",         "Terminal-based Wireshark",                             Category.SNIFFING_SPOOFING),
        new Tool("Tcpdump",      "tcpdump",        "Command-line packet analyzer",                         Category.SNIFFING_SPOOFING),
        new Tool("Ettercap",     "ettercap-graphical","Comprehensive MITM attack suite",                   Category.SNIFFING_SPOOFING),
        new Tool("Bettercap",    "bettercap",      "Network attack and monitoring framework",              Category.SNIFFING_SPOOFING),
        new Tool("Responder",    "responder",      "LLMNR/NBT-NS/MDNS poisoner",                           Category.SNIFFING_SPOOFING),
        new Tool("MITMproxy",    "mitmproxy",      "Interactive HTTPS proxy",                              Category.SNIFFING_SPOOFING),
        new Tool("Arpspoof",     "dsniff",         "ARP cache poisoning tool",                             Category.SNIFFING_SPOOFING),
        new Tool("dsniff",       "dsniff",         "Collection of network auditing tools",                 Category.SNIFFING_SPOOFING),
        new Tool("p0f",          "p0f",            "Passive OS fingerprinting",                            Category.SNIFFING_SPOOFING),
        new Tool("Sslstrip",     "sslstrip",       "SSL/TLS stripping attack tool",                        Category.SNIFFING_SPOOFING),
        new Tool("Scapy",        "scapy",          "Interactive packet manipulation library",              Category.SNIFFING_SPOOFING),
        new Tool("Netsniff-ng",  "netsniff-ng",    "High-performance network sniffer",                     Category.SNIFFING_SPOOFING),
        new Tool("Yersinia",     "yersinia",       "Layer 2 protocol attack tool",                         Category.SNIFFING_SPOOFING),

        // ---- Post Exploitation ----
        new Tool("Empire",       "powershell-empire","Post-exploitation framework",                        Category.POST_EXPLOITATION),
        new Tool("Impacket",     "python3-impacket","Windows protocol implementations",                    Category.POST_EXPLOITATION),
        new Tool("Evil-WinRM",   "evil-winrm",     "WinRM shell for hacking",                              Category.POST_EXPLOITATION),
        new Tool("BloodHound",   "bloodhound",     "Active Directory attack path mapping",                 Category.POST_EXPLOITATION),
        new Tool("Mimikatz",     "mimikatz",       "Windows credential extraction tool",                   Category.POST_EXPLOITATION),
        new Tool("Proxychains4", "proxychains4",   "Proxy chains for anonymizing traffic",                 Category.POST_EXPLOITATION),
        new Tool("Tor",          "tor",            "Anonymity network client",                             Category.POST_EXPLOITATION),
        new Tool("Ncat",         "ncat",           "Networking utility (Nmap version of netcat)",          Category.POST_EXPLOITATION),
        new Tool("Socat",        "socat",          "Multipurpose relay tool",                              Category.POST_EXPLOITATION),
        new Tool("PowerSploit",  "powersploit",    "PowerShell post-exploitation framework",               Category.POST_EXPLOITATION),
        new Tool("LinPEAS",      "peass-ng",       "Linux privilege escalation scripts",                   Category.POST_EXPLOITATION),
        new Tool("Pspy",         "pspy",           "Unprivileged Linux process snooping",                  Category.POST_EXPLOITATION),

        // ---- Forensics ----
        new Tool("Autopsy",      "autopsy",        "Digital forensics platform",                           Category.FORENSICS),
        new Tool("Binwalk",      "binwalk",        "Firmware analysis tool",                               Category.FORENSICS),
        new Tool("Foremost",     "foremost",       "File carving / data recovery tool",                   Category.FORENSICS),
        new Tool("Volatility",   "volatility3",    "Memory forensics framework",                           Category.FORENSICS),
        new Tool("dc3dd",        "dc3dd",          "Enhanced version of dd for forensics",                 Category.FORENSICS),
        new Tool("Scalpel",      "scalpel",        "Fast file carving and recovery tool",                  Category.FORENSICS),
        new Tool("Bulk Extractor","bulk-extractor", "Extracts features from disk images",                  Category.FORENSICS),
        new Tool("Exiftool",     "libimage-exiftool-perl","Metadata extraction tool",                      Category.FORENSICS),
        new Tool("Steghide",     "steghide",       "Steganography tool for JPEG/BMP/WAV/AU",              Category.FORENSICS),
        new Tool("Stegseek",     "stegseek",       "Fast steghide cracker",                               Category.FORENSICS),
        new Tool("Chkrootkit",   "chkrootkit",     "Locally checks for signs of rootkits",                Category.FORENSICS),
        new Tool("RKHunter",     "rkhunter",       "Rootkit hunter and scanner",                           Category.FORENSICS),

        // ---- Reverse Engineering ----
        new Tool("GDB",          "gdb",            "GNU debugger",                                         Category.REVERSE_ENGINEERING),
        new Tool("Radare2",      "radare2",        "Reverse engineering framework",                        Category.REVERSE_ENGINEERING),
        new Tool("Ghidra",       "ghidra",         "NSA software reverse engineering suite",              Category.REVERSE_ENGINEERING),
        new Tool("objdump",      "binutils",       "Binary file analyzer",                                 Category.REVERSE_ENGINEERING),
        new Tool("strings",      "binutils",       "Find printable strings in binary files",              Category.REVERSE_ENGINEERING),
        new Tool("strace",       "strace",         "System call tracer",                                   Category.REVERSE_ENGINEERING),
        new Tool("ltrace",       "ltrace",         "Library call tracer",                                  Category.REVERSE_ENGINEERING),
        new Tool("PEDA",         "gdb-peda",       "Python Exploit Development Assistance for GDB",       Category.REVERSE_ENGINEERING),
        new Tool("pwndbg",       "pwndbg",         "GDB plugin for exploit development",                  Category.REVERSE_ENGINEERING),
        new Tool("pwntools",     "python3-pwntools","CTF exploit development library",                     Category.REVERSE_ENGINEERING),
        new Tool("ROPgadget",    "python3-ropgadget","ROP chain builder",                                  Category.REVERSE_ENGINEERING),
        new Tool("Jadx",         "jadx",           "DEX/APK decompiler",                                   Category.REVERSE_ENGINEERING),
        new Tool("Apktool",      "apktool",        "Android APK reverse engineering tool",                Category.REVERSE_ENGINEERING),
        new Tool("Dex2jar",      "dex2jar",        "DEX to JAR converter",                                 Category.REVERSE_ENGINEERING),
        new Tool("JD-GUI",       "jd-gui",         "Java decompiler GUI",                                  Category.REVERSE_ENGINEERING),

        // ---- Social Engineering ----
        new Tool("SET",          "set",            "Social-Engineer Toolkit",                              Category.SOCIAL_ENGINEERING),
        new Tool("Gophish",      "gophish",        "Open-source phishing framework",                      Category.SOCIAL_ENGINEERING),
        new Tool("King Phisher", "king-phisher",   "Phishing campaign toolkit",                            Category.SOCIAL_ENGINEERING),
        new Tool("Shellphish",   "shellphish",     "Automated phishing tool",                              Category.SOCIAL_ENGINEERING),

        // ---- Android ----
        new Tool("ADB",          "adb",            "Android Debug Bridge",                                 Category.ANDROID),
        new Tool("Fastboot",     "fastboot",       "Android bootloader interface",                         Category.ANDROID),
        new Tool("Apktool",      "apktool",        "APK decompiler and recompiler",                       Category.ANDROID),
        new Tool("Frida",        "frida-tools",    "Dynamic instrumentation toolkit",                     Category.ANDROID),
        new Tool("Objection",    "objection",      "Runtime mobile exploration powered by Frida",         Category.ANDROID),
        new Tool("MobSF",        "mobsf",          "Mobile Security Framework",                            Category.ANDROID),
        new Tool("drozer",       "drozer",         "Android security assessment framework",                Category.ANDROID),
        new Tool("Android SDK",  "android-sdk",    "Android development kit tools",                        Category.ANDROID),

        // ---- Crypto ----
        new Tool("OpenSSL",      "openssl",        "Cryptography toolkit",                                 Category.CRYPTO),
        new Tool("GnuPG",        "gnupg",          "GNU Privacy Guard",                                    Category.CRYPTO),
        new Tool("Fcrackzip",    "fcrackzip",      "ZIP password cracker",                                 Category.CRYPTO),
        new Tool("PdfCrack",     "pdfcrack",       "PDF password recovery tool",                           Category.CRYPTO),
        new Tool("sslscan",      "sslscan",        "SSL/TLS configuration scanner",                        Category.CRYPTO),
        new Tool("sslyze",       "sslyze",         "SSL/TLS server analyzer",                              Category.CRYPTO),
        new Tool("testssl.sh",   "testssl.sh",     "SSL/TLS testing script",                               Category.CRYPTO),
        new Tool("GPGTools",     "gnupg2",         "GnuPG 2.x tools",                                      Category.CRYPTO)
    );

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static List<Tool> getAll() {
        return Collections.unmodifiableList(TOOLS);
    }

    public static List<Tool> getByCategory(Category category) {
        List<Tool> result = new ArrayList<>();
        for (Tool t : TOOLS) {
            if (t.category == category) result.add(t);
        }
        return result;
    }

    public static List<Tool> search(String query) {
        if (query == null || query.isEmpty()) return getAll();
        String q = query.toLowerCase().trim();
        List<Tool> result = new ArrayList<>();
        for (Tool t : TOOLS) {
            if (t.name.toLowerCase().contains(q) ||
                t.pkg.toLowerCase().contains(q)  ||
                t.description.toLowerCase().contains(q)) {
                result.add(t);
            }
        }
        return result;
    }

    public static Tool findByName(String name) {
        for (Tool t : TOOLS) {
            if (t.name.equalsIgnoreCase(name) || t.pkg.equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    public static int getTotalCount() {
        return TOOLS.size();
    }

    public static String getCategoryLabel(Category category) {
        switch (category) {
            case INFORMATION_GATHERING:  return "Information Gathering";
            case VULNERABILITY_ANALYSIS: return "Vulnerability Analysis";
            case WEB_APPLICATION:        return "Web Application";
            case DATABASE:               return "Database";
            case PASSWORD_ATTACKS:       return "Password Attacks";
            case WIRELESS:               return "Wireless";
            case REVERSE_ENGINEERING:    return "Reverse Engineering";
            case EXPLOITATION:           return "Exploitation";
            case SNIFFING_SPOOFING:      return "Sniffing & Spoofing";
            case POST_EXPLOITATION:      return "Post Exploitation";
            case FORENSICS:              return "Forensics";
            case REPORTING:              return "Reporting";
            case SOCIAL_ENGINEERING:     return "Social Engineering";
            case ANDROID:                return "Android";
            case CRYPTO:                 return "Cryptography";
            default:                     return "Other";
        }
    }
}
