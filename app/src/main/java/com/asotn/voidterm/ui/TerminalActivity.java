/*
 * VoidTerm - TerminalActivity
 * Main terminal screen. Crash-proof: catches Throwable not just Exception.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
package com.asotn.voidterm.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.asotn.voidterm.R;
import com.asotn.voidterm.engine.NativeTerminal;
import com.asotn.voidterm.terminal.CommandProcessor;
import com.asotn.voidterm.terminal.TabCompletionEngine;
import com.asotn.voidterm.terminal.TerminalSession;
import com.asotn.voidterm.utils.AppPreferences;
import com.asotn.voidterm.utils.EnvironmentManager;

import java.util.List;

public class TerminalActivity extends AppCompatActivity {

    private static final String TAG = "VoidTerm-Main";

    private static final int REQ_NOTIF          = 101;
    private static final int REQ_STORAGE        = 102;
    private static final int REQ_MANAGE_STORAGE = 103;

    // Terminal colors
    private static final int C_WHITE  = 0xFFE0E0E0;
    private static final int C_GREEN  = 0xFF00FF41;
    private static final int C_YELLOW = 0xFFFFCC00;
    private static final int C_RED    = 0xFFFF5555;
    private static final int C_GRAY   = 0xFF777777;
    private static final int C_PROMPT = 0xFF00FF41;
    private static final int C_CYAN   = 0xFF00CCCC;

    // Views
    private TextView         out;
    private EditText         input;
    private ScrollView       scroll;
    private ShellKeyboardView keyboard;

    // Session
    private TerminalSession     session;
    private TabCompletionEngine tabEngine;
    private final Handler       ui = new Handler(Looper.getMainLooper());

    // Tab state
    private List<String> tabCompletions;
    private String       tabPrefix;
    private int          tabIdx;

    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CRITICAL: catch Throwable (not just Exception) so UnsatisfiedLinkError
        // and other JVM Errors don't kill the process silently.
        try {
            setContentView(R.layout.activity_terminal);
            bindViews();
            requestNotifPermission();
            startSession();
            showWelcome();
        } catch (Throwable t) {
            Log.e(TAG, "onCreate failed: " + t, t);
            // Last resort — still show a usable (if bare) screen
            safeRecoverUI(t);
        }
    }

    // =========================================================================
    // UI setup
    // =========================================================================

    private void bindViews() {
        Toolbar tb = findViewById(R.id.toolbar);
        if (tb != null) {
            setSupportActionBar(tb);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("VoidTerm");
        }

        out      = findViewById(R.id.terminal_output);
        input    = findViewById(R.id.command_input);
        scroll   = findViewById(R.id.scroll_view);
        keyboard = findViewById(R.id.shell_keyboard);

        if (input == null || out == null) return; // layout broken — exit early

        out.setTextIsSelectable(true);

        // Apply saved font size
        float sp = AppPreferences.get(this).getFontSize();
        out.setTextSize(sp);
        input.setTextSize(sp);

        // Enter key sends command
        input.setOnEditorActionListener((v, id, ev) -> {
            if (id == EditorInfo.IME_ACTION_DONE ||
                (ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && ev.getAction() == KeyEvent.ACTION_DOWN)) {
                runCommand(); return true;
            }
            return false;
        });

        input.setOnKeyListener((v, code, ev) -> {
            if (ev.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (code == KeyEvent.KEYCODE_ENTER) { runCommand(); return true; }
            if (code == KeyEvent.KEYCODE_TAB)   { doTabComplete(); return true; }
            resetTab();
            return false;
        });

        // Clear button
        TextView clear = findViewById(R.id.btn_clear_input);
        if (clear != null) clear.setOnClickListener(v -> input.setText(""));

        // Extra keys strip
        if (keyboard != null) {
            keyboard.setOnKeyEventListener(new ShellKeyboardView.OnKeyEventListener() {
                @Override public void onKeyString(String text) { sendOrInsert(text); }
                @Override public void onCtrlKey(char key) {
                    if (session != null) session.sendInput(String.valueOf((char)(key & 0x1F)));
                }
                @Override public void onSpecialKey(ShellKeyboardView.SpecialKey sk) {
                    String esc = ShellKeyboardView.specialKeyToEscape(sk);
                    if (!esc.isEmpty() && session != null) session.sendInput(esc);
                }
            });
        }

        // Keep screen on if preferred
        if (AppPreferences.get(this).keepScreenOn())
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void sendOrInsert(String text) {
        if (session != null && session.isRunning()) {
            session.sendInput(text);
        } else if (input != null) {
            int pos = input.getSelectionStart();
            input.getText().insert(Math.max(0, pos), text);
        }
    }

    // =========================================================================
    // Session
    // =========================================================================

    private void startSession() {
        try {
            session = new TerminalSession(this);
            session.setOutputCallback(text ->
                ui.post(() -> { appendText(text, C_WHITE); bottom(); })
            );
            session.start();
            tabEngine = new TabCompletionEngine(null);
        } catch (Throwable t) {
            Log.e(TAG, "Session start error: " + t, t);
            appendColored("[VoidTerm] Could not start session: " + t.getMessage() + "\n", C_RED);
        }
    }

    // =========================================================================
    // Welcome banner
    // =========================================================================

    private void showWelcome() {
        appendColored(
            "  _  __     _ _ ____            _     _ \n" +
            " | |/ /__ _| (_)  _ \\_ __ ___ (_) __| |\n" +
            " | ' // _` | | | | | | '__/ _ \\| |/ _` |\n" +
            " | . \\ (_| | | | |_| | | | (_) | | (_| |\n" +
            " |_|\\_\\__,_|_|_|____/|_|  \\___/|_|\\__,_|\n\n", C_GREEN);
        appendColored("  Kali Linux Terminal for Android  v26.2\n", C_WHITE);
        appendColored("  Developer : github.com/Asotn  |  s.pi@outlook.sa\n", C_GRAY);
        appendColored("  License   : GPL-3.0  |  F-Droid Ready\n", C_GRAY);

        if (!NativeTerminal.isLibraryLoaded()) {
            appendColored("\n  [INFO] Java shell mode — build with NDK for full Kali support.\n", C_YELLOW);
        }
        if (!EnvironmentManager.isBootstrapped()) {
            appendColored("  [NOTE] Kali rootfs not set up. Type: apt-get update\n", C_YELLOW);
        }
        appendColored("\n  Type voidterm-help for commands.\n", C_CYAN);
        appendColored("  " + "─".repeat(50) + "\n\n", C_GRAY);
    }

    // =========================================================================
    // Command processing
    // =========================================================================

    private void runCommand() {
        if (input == null) return;
        String raw = input.getText().toString().trim();
        input.setText("");
        resetTab();

        if (raw.isEmpty()) {
            if (session != null && session.isRunning()) session.sendInput("\n");
            return;
        }

        appendColored("root@kali:~# ", C_PROMPT);
        appendColored(raw + "\n", C_WHITE);

        CommandProcessor.CommandResult r = CommandProcessor.process(raw, this);
        switch (r.type) {
            case FILE_PERMISSION: requestFilePermission(); break;
            case CLEAR:          if (out != null) out.setText(""); break;
            case EXIT:           showExitDlg(); break;
            case SETTINGS:       startActivity(new Intent(this, SettingsActivity.class)); break;
            case HELP:
            case ABOUT:
                if (r.output != null) appendColored(r.output.replace("\r\n", "\n"), C_WHITE);
                break;
            default:
                if (session != null && session.isRunning()) {
                    session.sendInput(raw + "\n");
                } else {
                    appendColored("[Session ended — open menu → restart]\n", C_RED);
                }
                break;
        }
        bottom();
    }

    // =========================================================================
    // Tab completion
    // =========================================================================

    private void doTabComplete() {
        if (tabEngine == null || input == null) return;
        String current = input.getText().toString();

        if (tabCompletions != null && !tabCompletions.isEmpty() && current.equals(tabPrefix)) {
            tabIdx = (tabIdx + 1) % tabCompletions.size();
            applyCompletion(tabCompletions.get(tabIdx));
            return;
        }

        tabEngine.complete(current, new TabCompletionEngine.CompletionCallback() {
            @Override public void onCompletions(String prefix, List<String> completions) {
                if (completions.isEmpty()) return;
                tabPrefix      = current;
                tabCompletions = completions;
                tabIdx         = 0;
                if (completions.size() == 1) {
                    applyCompletion(TabCompletionEngine.applyCompletion(current, prefix, completions.get(0)));
                } else {
                    String common = TabCompletionEngine.findCommonPrefix(completions);
                    if (!common.equals(prefix))
                        applyCompletion(TabCompletionEngine.applyCompletion(current, prefix, common));
                    StringBuilder sb = new StringBuilder("\n");
                    for (String c : completions) sb.append("  ").append(c).append("\n");
                    appendColored(sb.toString(), C_CYAN);
                    bottom();
                }
            }
            @Override public void onError(String msg) {}
        });
    }

    private void applyCompletion(String newText) {
        if (input == null) return;
        input.setText(newText);
        input.setSelection(newText.length());
    }

    private void resetTab() { tabCompletions = null; tabPrefix = null; tabIdx = 0; }

    // =========================================================================
    // Permissions
    // =========================================================================

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }

    private void requestFilePermission() {
        appendColored("Requesting file access...\n", C_YELLOW);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                    startActivityForResult(i, REQ_MANAGE_STORAGE);
                } else { appendColored("Already granted.\n", C_GREEN); }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            }
        } catch (Throwable t) { appendColored("Permission error: " + t.getMessage() + "\n", C_RED); }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] p, @NonNull int[] g) {
        super.onRequestPermissionsResult(req, p, g);
        if (req == REQ_STORAGE) {
            boolean ok = g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED;
            appendColored(ok ? "Storage access granted.\n" : "Storage access denied.\n",
                          ok ? C_GREEN : C_RED);
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_MANAGE_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean ok = Environment.isExternalStorageManager();
            appendColored(ok ? "Full file access granted.\n" : "Access not granted.\n",
                          ok ? C_GREEN : C_RED);
        }
    }

    // =========================================================================
    // Menu
    // =========================================================================

    @Override public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.menu_terminal, m); return true;
    }

    @Override public boolean onOptionsItemSelected(@NonNull MenuItem i) {
        int id = i.getItemId();
        if      (id == R.id.menu_clear)    { if (out != null) out.setText(""); return true; }
        else if (id == R.id.menu_ctrl_c)   { if (session != null) session.sendSignalInterrupt(); return true; }
        else if (id == R.id.menu_ctrl_d)   { if (session != null) session.sendInput("\u0004"); return true; }
        else if (id == R.id.menu_settings) { startActivity(new Intent(this, SettingsActivity.class)); return true; }
        else if (id == R.id.menu_about)    { startActivity(new Intent(this, AboutActivity.class)); return true; }
        return super.onOptionsItemSelected(i);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void appendText(String text, int color) { appendColored(text, color); }

    private void appendColored(String text, int color) {
        if (out == null || text == null) return;
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        sb.setSpan(new ForegroundColorSpan(color), 0, text.length(),
                   Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.append(sb);
    }

    private void bottom() {
        if (scroll != null) scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void showExitDlg() {
        new AlertDialog.Builder(this)
            .setTitle("Exit VoidTerm")
            .setMessage("Close the terminal?")
            .setPositiveButton("Exit",   (d, w) -> finish())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void safeRecoverUI(Throwable t) {
        try {
            if (out == null) {
                setContentView(R.layout.activity_terminal);
                out    = findViewById(R.id.terminal_output);
                input  = findViewById(R.id.command_input);
                scroll = findViewById(R.id.scroll_view);
            }
            if (out != null) out.setText("[VoidTerm] Startup error — " + t.getMessage() +
                "\nPlease report to: s.pi@outlook.sa");
        } catch (Throwable ignored) {}
    }

    @Override public void onBackPressed() { showExitDlg(); }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { if (session   != null) session.stop();   } catch (Throwable ignored) {}
        try { if (tabEngine != null) tabEngine.shutdown(); } catch (Throwable ignored) {}
    }
}
