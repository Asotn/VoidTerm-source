#!/bin/bash
# VoidTerm - Common Tools Installer
# Installs the most commonly used Kali Linux tools.
# Run inside the proot environment after setup_kali_environment.sh
#
# Usage:
#   ./install_common_tools.sh            - install all recommended tools
#   ./install_common_tools.sh network    - install network tools only
#   ./install_common_tools.sh web        - install web tools only
#   ./install_common_tools.sh passwords  - install password tools only
#   ./install_common_tools.sh wireless   - install wireless tools only
#   ./install_common_tools.sh forensics  - install forensics tools only
#
# Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
# License   : GPL-3.0

set -e
export DEBIAN_FRONTEND=noninteractive

log()  { echo "[VoidTerm] $*"; }
ok()   { echo "[  OK    ] $*"; }
warn() { echo "[ WARN   ] $*"; }

install_pkgs() {
    local category="$1"
    shift
    log "Installing $category..."
    apt-get install -y --no-install-recommends "$@" 2>&1 || \
        warn "Some packages in '$category' failed. Continuing."
    ok "$category installed"
}

# -------------------------------------------------------------------------
# Tool groups
# -------------------------------------------------------------------------

NETWORK_TOOLS=(
    nmap masscan hping3 fping netdiscover
    dnsenum dnsrecon fierce
    enum4linux nbtscan smbclient smbmap
    snmpwalk snmpcheck onesixtyone
    netcat-openbsd socat ncat
    traceroute whois
    arp-scan
    net-tools iputils-ping
)

WEB_TOOLS=(
    nikto dirb gobuster
    sqlmap
    wfuzz
    curl wget
    python3-requests
    libxml2-utils
    sslyze sslscan
    whatweb
)

PASSWORD_TOOLS=(
    john
    hashcat
    hydra
    medusa
    ncrack
    hash-identifier
    hashid
    cewl
    crunch
    wordlists
)

WIRELESS_TOOLS=(
    aircrack-ng
    kismet
    reaver
    bully
    pixiewps
    wifite
)

FORENSICS_TOOLS=(
    binwalk
    foremost
    exiftool
    steghide
    chkrootkit
    rkhunter
    autopsy
    dc3dd
    scalpel
)

REVERSING_TOOLS=(
    gdb
    radare2
    binutils
    strace
    ltrace
    elfutils
    patchelf
)

EXPLOITATION_TOOLS=(
    metasploit-framework
    exploitdb
)

SNIFFING_TOOLS=(
    tshark
    tcpdump
    ettercap-text-only
    bettercap
    responder
    dsniff
    scapy
    mitmproxy
)

GENERAL_TOOLS=(
    git
    curl
    wget
    tmux
    screen
    vim
    nano
    python3
    python3-pip
    python3-venv
    ruby
    perl
    golang
    gcc
    g++
    make
    cmake
    openssl
    gnupg
    unzip
    p7zip-full
    rsync
    netcat-openbsd
    socat
    tree
    jq
    xmlstarlet
    htop
    iftop
)

# -------------------------------------------------------------------------
# Main
# -------------------------------------------------------------------------
apt-get update -y

MODE="${1:-all}"

case "$MODE" in
    network)
        install_pkgs "Network Tools" "${NETWORK_TOOLS[@]}"
        ;;
    web)
        install_pkgs "Web Tools" "${WEB_TOOLS[@]}"
        ;;
    passwords)
        install_pkgs "Password Tools" "${PASSWORD_TOOLS[@]}"
        ;;
    wireless)
        install_pkgs "Wireless Tools" "${WIRELESS_TOOLS[@]}"
        ;;
    forensics)
        install_pkgs "Forensics Tools" "${FORENSICS_TOOLS[@]}"
        ;;
    reversing)
        install_pkgs "Reverse Engineering Tools" "${REVERSING_TOOLS[@]}"
        ;;
    exploitation)
        install_pkgs "Exploitation Tools" "${EXPLOITATION_TOOLS[@]}"
        ;;
    sniffing)
        install_pkgs "Sniffing Tools" "${SNIFFING_TOOLS[@]}"
        ;;
    general)
        install_pkgs "General Tools" "${GENERAL_TOOLS[@]}"
        ;;
    all|*)
        log "Installing all recommended tools..."
        apt-get update -y
        install_pkgs "General"       "${GENERAL_TOOLS[@]}"
        install_pkgs "Network"       "${NETWORK_TOOLS[@]}"
        install_pkgs "Web"           "${WEB_TOOLS[@]}"
        install_pkgs "Passwords"     "${PASSWORD_TOOLS[@]}"
        install_pkgs "Sniffing"      "${SNIFFING_TOOLS[@]}"
        install_pkgs "Forensics"     "${FORENSICS_TOOLS[@]}"
        install_pkgs "Reversing"     "${REVERSING_TOOLS[@]}"
        ;;
esac

apt-get autoremove -y 2>/dev/null || true
apt-get autoclean  2>/dev/null || true

echo ""
echo "========================================"
echo " VoidTerm - Tool installation complete"
echo " Developer: Asotn | github.com/Asotn"
echo "========================================"
