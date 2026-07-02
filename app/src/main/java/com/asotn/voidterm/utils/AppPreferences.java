/*
 * VoidTerm - AppPreferences
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {

    private static final String PREF_FILE = "voidterm_prefs";

    // Keys
    public static final String KEY_FONT_SIZE        = "font_size";
    public static final String KEY_FONT_FAMILY      = "font_family";
    public static final String KEY_SHOW_TOOLBAR     = "show_toolbar";
    public static final String KEY_KEEP_SCREEN_ON   = "keep_screen_on";
    public static final String KEY_VIBRATE_ON_KEY   = "vibrate_on_key";
    public static final String KEY_TERMINAL_COLS    = "terminal_cols";
    public static final String KEY_TERMINAL_ROWS    = "terminal_rows";
    public static final String KEY_SHELL_PATH       = "shell_path";
    public static final String KEY_DEFAULT_SHELL    = "default_shell";
    public static final String KEY_BOOTSTRAP_DONE   = "bootstrap_done";
    public static final String KEY_KALI_MIRROR      = "kali_mirror";
    public static final String KEY_HISTORY_SIZE     = "history_size";
    public static final String KEY_NOTIFICATION_PERM = "notification_perm_asked";

    // Defaults
    private static final int    DEFAULT_FONT_SIZE   = 14;
    private static final String DEFAULT_FONT_FAMILY = "monospace";
    private static final int    DEFAULT_COLS        = 80;
    private static final int    DEFAULT_ROWS        = 24;
    private static final int    DEFAULT_HISTORY_SIZE = 500;
    private static final String DEFAULT_SHELL       = "/bin/bash";

    private final SharedPreferences prefs;

    private static AppPreferences instance;

    public static AppPreferences get(Context ctx) {
        if (instance == null) {
            instance = new AppPreferences(ctx.getApplicationContext());
        }
        return instance;
    }

    private AppPreferences(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------
    public int    getFontSize()      { return prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE); }
    public String getFontFamily()    { return prefs.getString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY); }
    public boolean showToolbar()     { return prefs.getBoolean(KEY_SHOW_TOOLBAR, true); }
    public boolean keepScreenOn()    { return prefs.getBoolean(KEY_KEEP_SCREEN_ON, false); }
    public boolean vibrateOnKey()    { return prefs.getBoolean(KEY_VIBRATE_ON_KEY, false); }
    public int    getCols()          { return prefs.getInt(KEY_TERMINAL_COLS, DEFAULT_COLS); }
    public int    getRows()          { return prefs.getInt(KEY_TERMINAL_ROWS, DEFAULT_ROWS); }
    public String getShellPath()     { return prefs.getString(KEY_SHELL_PATH, DEFAULT_SHELL); }
    public String getDefaultShell()  { return prefs.getString(KEY_DEFAULT_SHELL, DEFAULT_SHELL); }
    public boolean isBootstrapDone() { return prefs.getBoolean(KEY_BOOTSTRAP_DONE, false); }
    public String getKaliMirror()    { return prefs.getString(KEY_KALI_MIRROR, EnvironmentManager.KALI_MIRROR); }
    public int    getHistorySize()   { return prefs.getInt(KEY_HISTORY_SIZE, DEFAULT_HISTORY_SIZE); }
    public boolean notifPermAsked()  { return prefs.getBoolean(KEY_NOTIFICATION_PERM, false); }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------
    public void setFontSize(int size)           { prefs.edit().putInt(KEY_FONT_SIZE, size).apply(); }
    public void setFontFamily(String family)    { prefs.edit().putString(KEY_FONT_FAMILY, family).apply(); }
    public void setShowToolbar(boolean show)    { prefs.edit().putBoolean(KEY_SHOW_TOOLBAR, show).apply(); }
    public void setKeepScreenOn(boolean keep)   { prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, keep).apply(); }
    public void setVibrateOnKey(boolean vib)    { prefs.edit().putBoolean(KEY_VIBRATE_ON_KEY, vib).apply(); }
    public void setCols(int cols)               { prefs.edit().putInt(KEY_TERMINAL_COLS, cols).apply(); }
    public void setRows(int rows)               { prefs.edit().putInt(KEY_TERMINAL_ROWS, rows).apply(); }
    public void setShellPath(String path)       { prefs.edit().putString(KEY_SHELL_PATH, path).apply(); }
    public void setBootstrapDone(boolean done)  { prefs.edit().putBoolean(KEY_BOOTSTRAP_DONE, done).apply(); }
    public void setKaliMirror(String mirror)    { prefs.edit().putString(KEY_KALI_MIRROR, mirror).apply(); }
    public void setHistorySize(int size)        { prefs.edit().putInt(KEY_HISTORY_SIZE, size).apply(); }
    public void setNotifPermAsked(boolean asked){ prefs.edit().putBoolean(KEY_NOTIFICATION_PERM, asked).apply(); }
}
