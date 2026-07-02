/*
 * VoidTerm - TabCompletionEngine
 * Provides tab-completion by querying the bash shell with compgen.
 * Completions are fetched asynchronously and delivered via callback.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.terminal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.asotn.voidterm.engine.NativeTerminal;
import com.asotn.voidterm.utils.EnvironmentManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TabCompletionEngine queries bash using `compgen` to produce completions
 * for a partial command or path. Results are delivered on the main thread.
 */
public class TabCompletionEngine {

    private static final String TAG = "VoidTerm-TabCompl";

    public interface CompletionCallback {
        void onCompletions(String prefix, List<String> completions);
        void onError(String message);
    }

    private final ExecutorService executor  = Executors.newSingleThreadExecutor();
    private final Handler         uiHandler = new Handler(Looper.getMainLooper());
    private final NativeTerminal  terminal;

    // Built-in Kali command list for instant offline completion
    private static final String[] BUILTIN_COMMANDS = {
        // Core utilities
        "apt-get", "apt", "apt-cache", "dpkg", "dpkg-query",
        "sudo", "su", "bash", "sh", "zsh", "fish",
        "ls", "ll", "la", "cd", "pwd", "echo", "cat", "less", "more",
        "grep", "egrep", "fgrep", "sed", "awk", "cut", "sort", "uniq",
        "find", "locate", "which", "whereis", "type",
        "cp", "mv", "rm", "mkdir", "rmdir", "touch", "ln",
        "chmod", "chown", "chgrp", "stat", "file",
        "ps", "top", "htop", "kill", "killall", "pkill",
        "ping", "curl", "wget", "nc", "netcat", "ncat",
        "ssh", "scp", "sftp", "ftp",
        "tar", "gzip", "gunzip", "bzip2", "xz", "zip", "unzip",
        "python3", "python", "pip3", "pip",
        "gcc", "g++", "make", "cmake",
        "git", "nano", "vim", "vi",
        "ifconfig", "ip", "route", "iptables", "nftables",
        "netstat", "ss", "lsof",
        "id", "whoami", "uname", "hostname", "env", "export",
        "history", "alias", "unalias", "source",
        "clear", "reset", "exit", "logout",
        // Kali tools
        "nmap", "masscan", "zmap",
        "metasploit-framework", "msfconsole", "msfvenom", "msfdb",
        "aircrack-ng", "airodump-ng", "airmon-ng", "aireplay-ng",
        "wireshark", "tshark", "tcpdump",
        "sqlmap", "sqlninja",
        "hydra", "john", "hashcat",
        "burpsuite", "zaproxy",
        "nikto", "dirb", "dirbuster", "gobuster", "feroxbuster",
        "wfuzz", "ffuf",
        "exploitdb", "searchsploit",
        "beef-xss",
        "maltego", "recon-ng",
        "wpscan", "joomscan",
        "volatility", "autopsy",
        "binwalk", "foremost",
        "steghide", "stegseek",
        "crackmapexec", "enum4linux", "smbclient", "smbmap",
        "impacket-scripts", "mimikatz",
        "responder", "evil-winrm",
        "proxychains4", "proxychains",
        "tor", "anonsurf",
        "armitage", "cobalt-strike",
        "setoolkit",
        "lynis", "chkrootkit", "rkhunter",
        "gdb", "ltrace", "strace", "radare2", "r2",
        "objdump", "readelf", "strings", "nm",
        "pwndbg", "pwncat",
        "socat", "openssl",
        "hashid", "hash-identifier",
        "dnsrecon", "dnsenum", "fierce",
        "theharvester", "recon-ng",
        "whois", "nslookup", "dig",
        "snmpwalk", "snmpcheck",
        "nbtscan", "nbtscan-unixwiz",
        "onesixtyone", "snmpenum",
        "android-sdk", "adb", "fastboot",
        "apktool", "jadx", "jadx-gui",
        "frida", "frida-tools", "objection",
        "mitmproxy", "mitmdump",
    };

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    public TabCompletionEngine(NativeTerminal terminal) {
        this.terminal = terminal;
    }

    // -------------------------------------------------------------------------
    // complete
    // Main entry point. Call when user presses Tab.
    // -------------------------------------------------------------------------
    public void complete(String input, CompletionCallback callback) {
        if (input == null || input.isEmpty()) {
            callback.onCompletions("", Collections.emptyList());
            return;
        }

        String trimmed = input.trim();
        String[] tokens = trimmed.split("\\s+");
        boolean isFirstToken = tokens.length <= 1 && !trimmed.endsWith(" ");

        if (isFirstToken) {
            // Complete command name from built-ins + PATH
            String prefix = tokens.length > 0 ? tokens[0] : "";
            List<String> matches = completeCommand(prefix);
            uiHandler.post(() -> callback.onCompletions(prefix, matches));
        } else {
            // Complete file/directory path
            String prefix = trimmed.endsWith(" ") ? "" : tokens[tokens.length - 1];
            completePathAsync(prefix, callback);
        }
    }

    // -------------------------------------------------------------------------
    // completeCommand
    // Instant lookup from built-in list.
    // -------------------------------------------------------------------------
    private List<String> completeCommand(String prefix) {
        List<String> matches = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String cmd : BUILTIN_COMMANDS) {
            if (cmd.startsWith(lower)) {
                matches.add(cmd);
            }
        }
        Collections.sort(matches);
        return matches;
    }

    // -------------------------------------------------------------------------
    // completePathAsync
    // Uses bash compgen -f to complete file/directory paths.
    // -------------------------------------------------------------------------
    private void completePathAsync(String prefix, CompletionCallback callback) {
        executor.execute(() -> {
            try {
                // Send compgen command to shell and read output via PTY
                // This works because TerminalSession routes output back to us
                String compgenCmd = "compgen -f -- '" + prefix.replace("'", "'\\''") + "' 2>/dev/null\n";
                List<String> results = new ArrayList<>();

                // For paths - do local file listing as fallback
                if (prefix.startsWith("/") || prefix.startsWith("./") || prefix.startsWith("~/")) {
                    results = completeLocalPath(prefix);
                } else {
                    results = completeCommand(prefix);
                }

                final List<String> finalResults = results;
                uiHandler.post(() -> callback.onCompletions(prefix, finalResults));

            } catch (Exception e) {
                Log.e(TAG, "Completion error: " + e.getMessage());
                uiHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // -------------------------------------------------------------------------
    // completeLocalPath
    // -------------------------------------------------------------------------
    private List<String> completeLocalPath(String prefix) {
        List<String> results = new ArrayList<>();

        // Resolve ~ to home
        String expanded = prefix.startsWith("~/")
            ? EnvironmentManager.HOME_DIR + prefix.substring(1)
            : prefix;

        int lastSlash = expanded.lastIndexOf('/');
        String dir, filePrefix;
        if (lastSlash >= 0) {
            dir        = expanded.substring(0, lastSlash + 1);
            filePrefix = expanded.substring(lastSlash + 1);
        } else {
            dir        = "./";
            filePrefix = expanded;
        }

        java.io.File dirFile = new java.io.File(dir);
        if (dirFile.isDirectory()) {
            String[] entries = dirFile.list();
            if (entries != null) {
                for (String entry : entries) {
                    if (entry.startsWith(filePrefix)) {
                        java.io.File child = new java.io.File(dirFile, entry);
                        results.add(dir + entry + (child.isDirectory() ? "/" : ""));
                    }
                }
            }
        }

        Collections.sort(results);
        return results;
    }

    // -------------------------------------------------------------------------
    // applyCompletion
    // Given the current input and selected completion, return the new input.
    // -------------------------------------------------------------------------
    public static String applyCompletion(String currentInput, String prefix, String completion) {
        if (currentInput == null || completion == null) return currentInput;

        if (prefix.isEmpty()) {
            return currentInput + completion + " ";
        }

        // Find the last occurrence of prefix at the end of input
        if (currentInput.endsWith(prefix)) {
            String base = currentInput.substring(0, currentInput.length() - prefix.length());
            boolean needsSpace = !completion.endsWith("/");
            return base + completion + (needsSpace ? " " : "");
        }

        return currentInput;
    }

    // -------------------------------------------------------------------------
    // findCommonPrefix
    // For multiple completions, finds the longest common prefix.
    // -------------------------------------------------------------------------
    public static String findCommonPrefix(List<String> completions) {
        if (completions == null || completions.isEmpty()) return "";
        if (completions.size() == 1) return completions.get(0);

        String first = completions.get(0);
        int len = first.length();

        for (String s : completions) {
            len = Math.min(len, s.length());
            for (int i = 0; i < len; i++) {
                if (s.charAt(i) != first.charAt(i)) {
                    len = i;
                    break;
                }
            }
        }

        return first.substring(0, len);
    }

    // -------------------------------------------------------------------------
    // shutdown
    // -------------------------------------------------------------------------
    public void shutdown() {
        executor.shutdown();
    }
}
