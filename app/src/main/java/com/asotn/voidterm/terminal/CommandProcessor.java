/*
 * VoidTerm - Command Processor
 * Handles built-in command interception: apt, sudo, ./files, help, clear, etc.
 * Commands that are not intercepted are passed straight to the PTY shell.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.terminal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.asotn.voidterm.utils.EnvironmentManager;

import java.io.File;
import java.util.Arrays;

public class CommandProcessor {

    private static final String TAG = "VoidTerm-CMD";

    public enum CommandType {
        PASSTHROUGH,       // send to PTY as-is
        FILE_PERMISSION,   // ./files -0 & permission
        CLEAR,             // clear the screen
        HELP,              // show help
        ABOUT,             // show about info
        SETTINGS,          // open settings
        EXIT               // exit the app
    }

    public static class CommandResult {
        public final CommandType type;
        public final String      output;     // local output to print (if any)
        public final String      passthroughCmd; // command to send to PTY

        public CommandResult(CommandType type, String output, String passthroughCmd) {
            this.type           = type;
            this.output         = output;
            this.passthroughCmd = passthroughCmd;
        }
    }

    // -------------------------------------------------------------------------
    // process
    // Entry point. Receives the raw string the user typed.
    // -------------------------------------------------------------------------
    public static CommandResult process(String raw, Context ctx) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return new CommandResult(CommandType.PASSTHROUGH, null, raw);
        }

        String lower = trimmed.toLowerCase();

        // File access permission command
        if (lower.equals("./files -0 & permission") ||
            lower.equals("./files -0 &permission")  ||
            lower.startsWith("./files -0")) {
            return new CommandResult(CommandType.FILE_PERMISSION, null, null);
        }

        // clear / cls
        if (lower.equals("clear") || lower.equals("cls")) {
            return new CommandResult(CommandType.CLEAR, null, null);
        }

        // exit / logout / quit
        if (lower.equals("exit") || lower.equals("logout") || lower.equals("quit")) {
            return new CommandResult(CommandType.EXIT, null, null);
        }

        // voidterm-help / help (only as standalone commands)
        if (lower.equals("voidterm-help") || lower.equals("voidterm --help")) {
            return new CommandResult(CommandType.HELP, buildHelpText(), null);
        }

        // voidterm-about
        if (lower.equals("voidterm-about") || lower.equals("about")) {
            return new CommandResult(CommandType.ABOUT, buildAboutText(), null);
        }

        // settings
        if (lower.equals("voidterm-settings") || lower.equals("settings")) {
            return new CommandResult(CommandType.SETTINGS, null, null);
        }

        // Everything else goes to the PTY shell (apt, sudo, pip, python, nmap, etc.)
        return new CommandResult(CommandType.PASSTHROUGH, null, raw);
    }

    // -------------------------------------------------------------------------
    // buildHelpText
    // -------------------------------------------------------------------------
    private static String buildHelpText() {
        return "\r\n" +
            "VoidTerm Terminal - Built-in Commands\r\n" +
            "======================================\r\n" +
            "\r\n" +
            "  apt install <pkg>       Install a Kali Linux package\r\n" +
            "  apt update              Update package lists\r\n" +
            "  apt upgrade             Upgrade installed packages\r\n" +
            "  apt remove <pkg>        Remove a package\r\n" +
            "  apt search <term>       Search for packages\r\n" +
            "  apt list --installed    List installed packages\r\n" +
            "\r\n" +
            "  sudo <command>          Run command as root\r\n" +
            "  su                      Switch to root shell\r\n" +
            "\r\n" +
            "  ./files -0 & permission Grant file access permission\r\n" +
            "\r\n" +
            "  clear                   Clear the terminal screen\r\n" +
            "  exit                    Close the terminal\r\n" +
            "  voidterm-about         Show developer info\r\n" +
            "  voidterm-settings      Open app settings\r\n" +
            "  voidterm-help          Show this help message\r\n" +
            "\r\n" +
            "  Standard Unix commands are fully supported.\r\n" +
            "  This terminal runs a real bash shell inside a Kali Linux\r\n" +
            "  environment via proot. All Kali tools are available.\r\n" +
            "\r\n";
    }

    // -------------------------------------------------------------------------
    // buildAboutText
    // -------------------------------------------------------------------------
    private static String buildAboutText() {
        return "\r\n" +
            "VoidTerm Terminal v26.2\r\n" +
            "=========================\r\n" +
            "\r\n" +
            "  Developer  : Asotn\r\n" +
            "  GitHub     : https://github.com/Asotn\r\n" +
            "  Email      : s.pi@outlook.sa\r\n" +
            "  License    : GPL-3.0\r\n" +
            "  F-Droid    : Ready\r\n" +
            "\r\n" +
            "  Running on : " + Build.MANUFACTURER + " " + Build.MODEL + "\r\n" +
            "  Android    : " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\r\n" +
            "  ABI        : " + EnvironmentManager.getPrimaryAbi() + "\r\n" +
            "  Kali arch  : " + EnvironmentManager.getKaliArch() + "\r\n" +
            "\r\n" +
            "  A real Kali Linux terminal for Android.\r\n" +
            "  Powered by proot + bash + apt.\r\n" +
            "\r\n";
    }
}
