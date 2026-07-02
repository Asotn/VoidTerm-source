/*
 * VoidTerm - ShellKeyboardView
 * A horizontal strip of extra keys displayed above the software keyboard.
 * Provides: ESC, Tab, Ctrl, |, /, -, ~, arrows, and common terminal keys.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.asotn.voidterm.utils.AppPreferences;

/**
 * A scrollable row of special keys designed for terminal use on mobile.
 * Attach an OnKeyEventListener to receive key events.
 */
public class ShellKeyboardView extends HorizontalScrollView {

    public interface OnKeyEventListener {
        void onKeyString(String text);
        void onCtrlKey(char key);
        void onSpecialKey(SpecialKey key);
    }

    public enum SpecialKey {
        ESC, TAB, UP, DOWN, LEFT, RIGHT,
        PAGE_UP, PAGE_DOWN, HOME, END,
        CTRL_C, CTRL_D, CTRL_Z, CTRL_L
    }

    private LinearLayout        container;
    private OnKeyEventListener  listener;
    private boolean             ctrlMode = false;
    private Button              ctrlButton;

    // Key definitions: label -> action
    private static final Object[][] KEYS = {
        // label       type         payload
        { "ESC",      "special",   SpecialKey.ESC       },
        { "Tab",      "special",   SpecialKey.TAB       },
        { "Ctrl",     "ctrl_toggle", null               },
        { "^C",       "special",   SpecialKey.CTRL_C    },
        { "^D",       "special",   SpecialKey.CTRL_D    },
        { "^Z",       "special",   SpecialKey.CTRL_Z    },
        { "^L",       "special",   SpecialKey.CTRL_L    },
        { "|",        "string",    "|"                  },
        { "/",        "string",    "/"                  },
        { "~",        "string",    "~"                  },
        { "-",        "string",    "-"                  },
        { "_",        "string",    "_"                  },
        { ".",        "string",    "."                  },
        { "*",        "string",    "*"                  },
        { "\"",       "string",    "\""                 },
        { "'",        "string",    "'"                  },
        { "`",        "string",    "`"                  },
        { "&",        "string",    "&"                  },
        { ";",        "string",    ";"                  },
        { "&&",       "string",    " && "               },
        { "||",       "string",    " || "               },
        { ">>",       "string",    " >> "               },
        { ">",        "string",    " > "                },
        { "<",        "string",    " < "                },
        { "2>",       "string",    " 2> "               },
        { "2>&1",     "string",    " 2>&1"              },
        { "Up",       "special",   SpecialKey.UP        },
        { "Dn",       "special",   SpecialKey.DOWN      },
        { "Lt",       "special",   SpecialKey.LEFT      },
        { "Rt",       "special",   SpecialKey.RIGHT     },
        { "Home",     "special",   SpecialKey.HOME      },
        { "End",      "special",   SpecialKey.END       },
        { "PgUp",     "special",   SpecialKey.PAGE_UP   },
        { "PgDn",     "special",   SpecialKey.PAGE_DOWN },
    };

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ShellKeyboardView(Context context) {
        super(context);
        init(context);
    }

    public ShellKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------
    private void init(Context context) {
        setBackgroundColor(Color.parseColor("#0A0A0A"));
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);

        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(4, 0, 4, 0);

        for (Object[] keyDef : KEYS) {
            String label   = (String) keyDef[0];
            String type    = (String) keyDef[1];
            Object payload = keyDef[2];

            Button btn = createKey(context, label);

            if ("ctrl_toggle".equals(type)) {
                ctrlButton = btn;
                btn.setOnClickListener(v -> {
                    ctrlMode = !ctrlMode;
                    btn.setTextColor(ctrlMode
                        ? Color.parseColor("#00FF77")
                        : Color.parseColor("#AAAAAA"));
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                });
            } else if ("string".equals(type)) {
                final String text = (String) payload;
                btn.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (ctrlMode && text.length() == 1) {
                        char c = text.charAt(0);
                        if (listener != null) listener.onCtrlKey(c);
                        ctrlMode = false;
                        if (ctrlButton != null)
                            ctrlButton.setTextColor(Color.parseColor("#AAAAAA"));
                    } else {
                        if (listener != null) listener.onKeyString(text);
                    }
                });
            } else if ("special".equals(type)) {
                final SpecialKey sk = (SpecialKey) payload;
                btn.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (listener != null) listener.onSpecialKey(sk);
                });
            }

            container.addView(btn);
        }

        addView(container);
    }

    // -------------------------------------------------------------------------
    // createKey
    // -------------------------------------------------------------------------
    private Button createKey(Context context, String label) {
        Button btn = new Button(context);
        btn.setText(label);
        btn.setTextColor(Color.parseColor("#AAAAAA"));
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.MONOSPACE);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(2, 2, 2, 2);
        btn.setLayoutParams(lp);
        btn.setPadding(18, 4, 18, 4);
        btn.setMinWidth(0);
        btn.setMinimumWidth(0);
        btn.setBackgroundColor(Color.parseColor("#1A1A1A"));

        return btn;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------
    public void setOnKeyEventListener(OnKeyEventListener l) {
        this.listener = l;
    }

    public boolean isCtrlMode() {
        return ctrlMode;
    }

    public void resetCtrlMode() {
        ctrlMode = false;
        if (ctrlButton != null)
            ctrlButton.setTextColor(Color.parseColor("#AAAAAA"));
    }

    /**
     * Returns the ANSI escape sequence for special keys so they can be
     * sent directly to the PTY shell.
     */
    public static String specialKeyToEscape(SpecialKey key) {
        switch (key) {
            case ESC:       return "\u001b";
            case TAB:       return "\t";
            case UP:        return "\u001b[A";
            case DOWN:      return "\u001b[B";
            case RIGHT:     return "\u001b[C";
            case LEFT:      return "\u001b[D";
            case HOME:      return "\u001b[H";
            case END:       return "\u001b[F";
            case PAGE_UP:   return "\u001b[5~";
            case PAGE_DOWN: return "\u001b[6~";
            case CTRL_C:    return "\u0003";
            case CTRL_D:    return "\u0004";
            case CTRL_Z:    return "\u001a";
            case CTRL_L:    return "\u000c";
            default:        return "";
        }
    }
}
