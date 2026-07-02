#!/bin/sh
# VoidTerm Bootstrap Script
# Sets up the Kali Linux environment after rootfs extraction.
# Runs inside proot as root.
#
# Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
# License   : GPL-3.0

set -e

log() {
    echo "[VoidTerm] $1"
}

log "Starting Kali environment bootstrap..."

# Fix /etc/resolv.conf
log "Configuring DNS..."
echo "nameserver 8.8.8.8" > /etc/resolv.conf
echo "nameserver 8.8.4.4" >> /etc/resolv.conf

# Fix /etc/hosts
if [ ! -f /etc/hosts ]; then
    log "Creating /etc/hosts..."
    echo "127.0.0.1   localhost" > /etc/hosts
    echo "::1         localhost" >> /etc/hosts
fi

# Create required directories
log "Creating directories..."
mkdir -p /root /tmp /run /proc /sys /dev /var/run /var/log

# Set HOME
export HOME=/root
export USER=root
export LOGNAME=root
export TERM=xterm-256color
export LANG=en_US.UTF-8
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# Set up Kali APT sources
log "Configuring APT sources..."
mkdir -p /etc/apt
cat > /etc/apt/sources.list << 'EOF'
deb http://http.kali.org/kali kali-rolling main contrib non-free non-free-firmware
EOF

# Disable recommended/suggested package auto-install for speed
mkdir -p /etc/apt/apt.conf.d
cat > /etc/apt/apt.conf.d/99voidterm << 'EOF'
APT::Install-Recommends "false";
APT::Install-Suggests "false";
Dpkg::Options:: "--force-confnew";
EOF

# Create a minimal /etc/environment
log "Setting environment..."
cat > /etc/environment << 'EOF'
LANG=en_US.UTF-8
LC_ALL=en_US.UTF-8
TERM=xterm-256color
DEBIAN_FRONTEND=noninteractive
EOF

# Create root .bashrc with VoidTerm prompt
log "Configuring bash..."
cat > /root/.bashrc << 'BASHRC'
# VoidTerm bash configuration
# Developer: Asotn | https://github.com/Asotn

export PS1="\[\033[01;31m\]root@kali\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]# "
export TERM=xterm-256color
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
export DEBIAN_FRONTEND=noninteractive
export HISTSIZE=1000
export HISTFILESIZE=2000
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games

# Aliases
alias ls='ls --color=auto'
alias ll='ls -alF'
alias la='ls -A'
alias l='ls -CF'
alias grep='grep --color=auto'
alias apt='apt-get'
alias update='apt-get update'
alias upgrade='apt-get upgrade -y'
alias install='apt-get install -y'
alias search='apt-cache search'
alias purge='apt-get autoremove --purge -y'

# VoidTerm helper
alias kali-help='echo "VoidTerm Terminal | github.com/Asotn | s.pi@outlook.sa"'
BASHRC

# Create root .profile
cat > /root/.profile << 'PROFILE'
if [ -n "$BASH_VERSION" ]; then
    if [ -f "$HOME/.bashrc" ]; then
        . "$HOME/.bashrc"
    fi
fi
PROFILE

log "Bootstrap complete."
log "Run 'apt-get update' to refresh package lists."
log "Then 'apt-get install <package>' to install tools."
echo ""
echo "Welcome to VoidTerm - Kali Linux on Android"
echo "Developer: github.com/Asotn | s.pi@outlook.sa"
echo ""
