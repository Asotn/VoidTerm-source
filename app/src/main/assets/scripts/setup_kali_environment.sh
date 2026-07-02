#!/bin/bash
# VoidTerm - Kali Linux Environment Full Setup Script
# Runs inside the proot Kali environment after first-time bootstrap.
# Sets up: locales, apt config, SSH, Python, common tools baseline.
#
# Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
# License   : GPL-3.0

set -e
LOG_FILE="/var/log/voidterm_setup.log"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# -------------------------------------------------------------------------
# Logging helpers
# -------------------------------------------------------------------------
log_info()  { echo "[INFO ] $*" | tee -a "$LOG_FILE"; }
log_ok()    { echo "[  OK ] $*" | tee -a "$LOG_FILE"; }
log_warn()  { echo "[ WARN] $*" | tee -a "$LOG_FILE"; }
log_error() { echo "[ERROR] $*" | tee -a "$LOG_FILE"; }
log_step()  { echo "" | tee -a "$LOG_FILE"; echo "==> $*" | tee -a "$LOG_FILE"; }

# -------------------------------------------------------------------------
# Header
# -------------------------------------------------------------------------
echo "============================================================"
echo " VoidTerm Environment Setup"
echo " Developer: Asotn | github.com/Asotn | s.pi@outlook.sa"
echo " Started: $TIMESTAMP"
echo "============================================================"
echo "" | tee -a "$LOG_FILE"

# -------------------------------------------------------------------------
# Step 1: Configure environment
# -------------------------------------------------------------------------
log_step "Configuring environment variables"

export DEBIAN_FRONTEND=noninteractive
export HOME=/root
export USER=root
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

log_ok "Environment configured"

# -------------------------------------------------------------------------
# Step 2: Fix DNS
# -------------------------------------------------------------------------
log_step "Configuring DNS"
echo "nameserver 8.8.8.8" > /etc/resolv.conf
echo "nameserver 8.8.4.4" >> /etc/resolv.conf
log_ok "DNS configured (8.8.8.8, 8.8.4.4)"

# -------------------------------------------------------------------------
# Step 3: Update APT
# -------------------------------------------------------------------------
log_step "Updating package lists"
apt-get update -y 2>&1 | tee -a "$LOG_FILE" || {
    log_warn "apt update had errors (mirror might be slow). Continuing..."
}

# -------------------------------------------------------------------------
# Step 4: Install essential base packages
# -------------------------------------------------------------------------
log_step "Installing essential packages"
ESSENTIAL_PKGS=(
    apt-utils
    locales
    ca-certificates
    curl
    wget
    git
    nano
    vim
    less
    file
    lsof
    net-tools
    iputils-ping
    dnsutils
    netcat-openbsd
    socat
    bash-completion
    man-db
    sudo
    python3
    python3-pip
    python3-setuptools
    python3-venv
    openssh-client
    openssl
    gnupg
    unzip
    p7zip-full
    tar
    gzip
    bzip2
    xz-utils
    procps
    psmisc
    htop
    tmux
    screen
    proot
)

apt-get install -y --no-install-recommends "${ESSENTIAL_PKGS[@]}" 2>&1 | tee -a "$LOG_FILE"
log_ok "Essential packages installed"

# -------------------------------------------------------------------------
# Step 5: Configure locales
# -------------------------------------------------------------------------
log_step "Configuring locales"
echo "en_US.UTF-8 UTF-8" > /etc/locale.gen
locale-gen 2>&1 | tee -a "$LOG_FILE" || log_warn "locale-gen failed (non-critical)"
echo "LANG=en_US.UTF-8" > /etc/default/locale
log_ok "Locales configured"

# -------------------------------------------------------------------------
# Step 6: Configure bash
# -------------------------------------------------------------------------
log_step "Configuring bash shell"
cat > /root/.bashrc << 'BASHRC'
# VoidTerm .bashrc - Asotn | github.com/Asotn

# Prompt: root@kali:~/path#
PS1='\[\033[01;31m\]root\[\033[00m\]@\[\033[01;32m\]kali\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '

# Environment
export TERM=xterm-256color
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8
export HISTSIZE=5000
export HISTFILESIZE=10000
export HISTCONTROL=ignoredups:ignorespace
export DEBIAN_FRONTEND=noninteractive
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games

# Shell options
shopt -s histappend
shopt -s checkwinsize
shopt -s cdspell
shopt -s dirspell
shopt -s globstar

# Aliases - core
alias ls='ls --color=auto'
alias ll='ls -alFh'
alias la='ls -A'
alias l='ls -CF'
alias ..='cd ..'
alias ...='cd ../..'
alias ....='cd ../../..'
alias grep='grep --color=auto'
alias fgrep='fgrep --color=auto'
alias egrep='egrep --color=auto'
alias diff='diff --color=auto'
alias ip='ip --color=auto'
alias cls='clear'

# Aliases - apt
alias apt='apt-get'
alias update='apt-get update'
alias upgrade='apt-get upgrade -y'
alias install='apt-get install -y'
alias remove='apt-get remove -y'
alias purge='apt-get purge -y'
alias search='apt-cache search'
alias show='apt-cache show'
alias autoremove='apt-get autoremove -y'
alias autoclean='apt-get autoclean'

# Aliases - network
alias myip='curl -s ifconfig.me'
alias localip='ip -4 addr show | grep inet'
alias ports='netstat -tulanp'
alias listening='ss -tlnp'

# Aliases - misc
alias path='echo $PATH | tr ":" "\n"'
alias now='date "+%Y-%m-%d %H:%M:%S"'
alias diskusage='du -sh * | sort -h'
alias meminfo='free -h'
alias cpuinfo='lscpu'

# Python shortcuts
alias py='python3'
alias pip='pip3'

# VoidTerm info
alias kali-version='cat /etc/os-release'
alias kali-tools='echo "Run: voidterm-help for tool listing"'
alias about='echo "VoidTerm | Developer: Asotn | github.com/Asotn | s.pi@outlook.sa"'

# Enable bash completion
if [ -f /etc/bash_completion ]; then
    . /etc/bash_completion
fi

# Welcome message
echo ""
echo "  VoidTerm - Kali Linux Terminal"
echo "  Developer: Asotn | github.com/Asotn"
echo "  Type 'about' for info | 'install <pkg>' to add tools"
echo ""
BASHRC

log_ok "Bash configured"

# -------------------------------------------------------------------------
# Step 7: Configure pip
# -------------------------------------------------------------------------
log_step "Configuring Python pip"
pip3 config set global.break-system-packages true 2>/dev/null || true
mkdir -p ~/.config/pip
cat > ~/.config/pip/pip.conf << 'EOF'
[global]
break-system-packages = true
EOF
log_ok "pip configured"

# -------------------------------------------------------------------------
# Step 8: Setup SSH client config
# -------------------------------------------------------------------------
log_step "Configuring SSH client"
mkdir -p /root/.ssh
chmod 700 /root/.ssh
cat > /root/.ssh/config << 'EOF'
Host *
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
    ServerAliveInterval 60
    ServerAliveCountMax 3
    LogLevel ERROR
EOF
chmod 600 /root/.ssh/config
log_ok "SSH client configured"

# -------------------------------------------------------------------------
# Step 9: Setup tmux
# -------------------------------------------------------------------------
log_step "Configuring tmux"
cat > /root/.tmux.conf << 'EOF'
# VoidTerm tmux config
set -g default-terminal "xterm-256color"
set -g history-limit 10000
set -g mouse on
set -g base-index 1
set -g pane-base-index 1
set -g status-bg black
set -g status-fg green
set -g status-left "[VoidTerm] "
set -g status-right "%H:%M %d-%b-%y"
bind r source-file ~/.tmux.conf \; display "Config reloaded"
EOF
log_ok "tmux configured"

# -------------------------------------------------------------------------
# Step 10: Setup vim
# -------------------------------------------------------------------------
log_step "Configuring vim"
cat > /root/.vimrc << 'EOF'
syntax on
set number
set autoindent
set tabstop=4
set shiftwidth=4
set expandtab
set background=dark
set hlsearch
set incsearch
set ruler
set showcmd
set wildmenu
colorscheme desert
EOF
log_ok "vim configured"

# -------------------------------------------------------------------------
# Step 11: Create useful directories
# -------------------------------------------------------------------------
log_step "Creating workspace directories"
mkdir -p /root/{tools,wordlists,targets,scripts,loot,reports}
mkdir -p /usr/share/wordlists

# Create a README
cat > /root/README.txt << 'REOF'
VoidTerm Workspace
===================
tools/      - Downloaded/compiled tools
wordlists/  - Password and wordlist files
targets/    - Target information and notes
scripts/    - Custom scripts
loot/       - Captured credentials and data
reports/    - Penetration testing reports

Common commands:
  apt-get update               Update package lists
  apt-get install -y <pkg>     Install a package
  apt-cache search <term>      Search for packages
  about                        Show developer info

Developer: Asotn | github.com/Asotn | s.pi@outlook.sa
REOF

log_ok "Workspace directories created"

# -------------------------------------------------------------------------
# Step 12: Final status
# -------------------------------------------------------------------------
log_step "Setup complete"
echo ""
echo "============================================================"
echo " VoidTerm environment setup COMPLETE"
echo " $(date '+%Y-%m-%d %H:%M:%S')"
echo ""
echo " Run 'apt-get install <tool>' to install Kali tools"
echo " Run 'source ~/.bashrc' to reload shell config"
echo ""
echo " Developer: Asotn | github.com/Asotn | s.pi@outlook.sa"
echo "============================================================"
echo ""
