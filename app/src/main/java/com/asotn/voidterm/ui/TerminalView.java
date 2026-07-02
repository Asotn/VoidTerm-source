/*
 * VoidTerm - TerminalView
 * Custom Android View that renders the VT100 screen buffer cell-by-cell.
 * Supports ANSI 16-color, 256-color, and 24-bit true-color rendering.
 * Handles selection, long-press copy, and pinch-to-zoom font size.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import com.asotn.voidterm.utils.AppPreferences;

/**
 * TerminalView renders text output in a monospace grid, suitable for
 * terminal emulator display. This view is used alongside the ScrollView
 * + TextView approach as an optional high-fidelity renderer.
 *
 * The terminal content is fed as a sequence of lines (strings), each
 * containing raw text (ANSI codes already stripped). For full ANSI
 * rendering with color, the NativeTerminal's VT100 buffer is used.
 */
public class TerminalView extends View {

    private static final String TAG = "VoidTerm-TermView";

    // Rendering
    private Paint        textPaint;
    private Paint        bgPaint;
    private Paint        cursorPaint;
    private Paint        selectionPaint;
    private float        charWidth;
    private float        charHeight;
    private float        charAscent;
    private float        fontSize;

    // Grid dimensions
    private int cols = 80;
    private int rows = 24;

    // Screen buffer: rows of char arrays
    private char[][]  screenChars;
    private int[][]   screenFg;    // foreground color per cell (ARGB)
    private int[][]   screenBg;    // background color per cell (ARGB)
    private boolean[][] screenBold;

    // Cursor position
    private int cursorRow = 0;
    private int cursorCol = 0;
    private boolean cursorVisible = true;
    private boolean cursorBlink   = true;

    // Selection state
    private int selStartRow = -1, selStartCol = -1;
    private int selEndRow   = -1, selEndCol   = -1;
    private boolean isSelecting = false;

    // Gesture detectors
    private GestureDetector      gestureDetector;
    private ScaleGestureDetector scaleDetector;

    // Cursor blink handler
    private final Handler blinkHandler = new Handler(Looper.getMainLooper());
    private final Runnable blinkRunnable = () -> {
        cursorVisible = !cursorVisible;
        invalidate();
        blinkHandler.postDelayed(blinkRunnable, 500);
    };

    // Callbacks
    public interface OnCellTapListener {
        void onCellTap(int row, int col);
    }
    private OnCellTapListener tapListener;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TerminalView(Context context) {
        super(context);
        init(context);
    }

    public TerminalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TerminalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------
    private void init(Context context) {
        fontSize = AppPreferences.get(context).getFontSize();

        // Background paint
        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setStyle(Paint.Style.FILL);

        // Text paint - monospace
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setTextSize(fontSize * getResources().getDisplayMetrics().density);
        textPaint.setColor(Color.parseColor("#E0E0E0"));
        textPaint.setSubpixelText(true);
        textPaint.setLinearText(true);

        // Cursor paint
        cursorPaint = new Paint();
        cursorPaint.setColor(Color.parseColor("#00FF77"));
        cursorPaint.setStyle(Paint.Style.FILL);
        cursorPaint.setAlpha(180);

        // Selection paint
        selectionPaint = new Paint();
        selectionPaint.setColor(Color.parseColor("#3300FF77"));
        selectionPaint.setStyle(Paint.Style.FILL);

        measureChar();
        allocateBuffer(rows, cols);

        // Gesture detector for long-press copy and tap
        gestureDetector = new GestureDetector(context,
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    startSelection(e);
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    clearSelection();
                    if (tapListener != null) {
                        int row = (int)(e.getY() / charHeight);
                        int col = (int)(e.getX() / charWidth);
                        tapListener.onCellTap(row, col);
                    }
                    return true;
                }
            });

        // Pinch-to-zoom for font size
        scaleDetector = new ScaleGestureDetector(context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    fontSize *= detector.getScaleFactor();
                    fontSize = Math.max(8f, Math.min(24f, fontSize));
                    textPaint.setTextSize(fontSize * getResources().getDisplayMetrics().density);
                    measureChar();
                    requestLayout();
                    invalidate();
                    return true;
                }
            });

        setLayerType(LAYER_TYPE_HARDWARE, null);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    // -------------------------------------------------------------------------
    // measureChar
    // -------------------------------------------------------------------------
    private void measureChar() {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        charHeight = fm.descent - fm.ascent;
        charAscent = -fm.ascent;
        charWidth  = textPaint.measureText("M");
    }

    // -------------------------------------------------------------------------
    // allocateBuffer
    // -------------------------------------------------------------------------
    private void allocateBuffer(int rows, int cols) {
        screenChars = new char[rows][cols];
        screenFg    = new int[rows][cols];
        screenBg    = new int[rows][cols];
        screenBold  = new boolean[rows][cols];

        int defaultFg = Color.parseColor("#E0E0E0");
        int defaultBg = Color.BLACK;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                screenChars[r][c] = ' ';
                screenFg[r][c]    = defaultFg;
                screenBg[r][c]    = defaultBg;
            }
        }
    }

    // -------------------------------------------------------------------------
    // setCell
    // -------------------------------------------------------------------------
    public void setCell(int row, int col, char ch, int fg, int bg, boolean bold) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        screenChars[row][col] = ch;
        screenFg[row][col]    = fg;
        screenBg[row][col]    = bg;
        screenBold[row][col]  = bold;
    }

    // -------------------------------------------------------------------------
    // setCursorPosition
    // -------------------------------------------------------------------------
    public void setCursorPosition(int row, int col) {
        cursorRow = Math.max(0, Math.min(row, rows - 1));
        cursorCol = Math.max(0, Math.min(col, cols - 1));
    }

    // -------------------------------------------------------------------------
    // setDimensions
    // -------------------------------------------------------------------------
    public void setDimensions(int cols, int rows) {
        if (cols == this.cols && rows == this.rows) return;
        this.cols = cols;
        this.rows = rows;
        allocateBuffer(rows, cols);
        requestLayout();
        invalidate();
    }

    // -------------------------------------------------------------------------
    // getDimensions
    // -------------------------------------------------------------------------
    public int getTermCols() {
        return getWidth() > 0 ? (int)(getWidth()  / charWidth)  : cols;
    }

    public int getTermRows() {
        return getHeight() > 0 ? (int)(getHeight() / charHeight) : rows;
    }

    // -------------------------------------------------------------------------
    // onMeasure
    // -------------------------------------------------------------------------
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = (int)(cols * charWidth);
        int h = (int)(rows * charHeight);
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(h, heightMeasureSpec)
        );
    }

    // -------------------------------------------------------------------------
    // onDraw
    // -------------------------------------------------------------------------
    @Override
    protected void onDraw(Canvas canvas) {
        // Fill background
        canvas.drawColor(Color.BLACK);

        if (screenChars == null) return;

        char[] singleChar = new char[1];
        Rect   cellRect   = new Rect();

        for (int r = 0; r < rows && r < screenChars.length; r++) {
            float top    = r * charHeight;
            float bottom = top + charHeight;

            for (int c = 0; c < cols && c < screenChars[r].length; c++) {
                float left  = c * charWidth;
                float right = left + charWidth;

                // Background
                int bg = screenBg[r][c];
                if (bg != Color.BLACK) {
                    bgPaint.setColor(bg);
                    canvas.drawRect(left, top, right, bottom, bgPaint);
                }

                // Selection highlight
                if (isCellSelected(r, c)) {
                    canvas.drawRect(left, top, right, bottom, selectionPaint);
                }

                // Cursor
                if (r == cursorRow && c == cursorCol && cursorVisible) {
                    canvas.drawRect(left, top, right, bottom, cursorPaint);
                }

                // Character
                char ch = screenChars[r][c];
                if (ch != ' ' && ch != '\0') {
                    int fg = screenFg[r][c];
                    textPaint.setColor(fg);
                    textPaint.setFakeBoldText(screenBold[r][c]);
                    singleChar[0] = ch;
                    canvas.drawText(singleChar, 0, 1, left, top + charAscent, textPaint);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // onTouchEvent
    // -------------------------------------------------------------------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        if (!scaleDetector.isInProgress()) {
            gestureDetector.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_UP && isSelecting) {
            finishSelection(event);
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Selection helpers
    // -------------------------------------------------------------------------
    private void startSelection(MotionEvent e) {
        isSelecting  = true;
        selStartRow  = (int)(e.getY() / charHeight);
        selStartCol  = (int)(e.getX() / charWidth);
        selEndRow    = selStartRow;
        selEndCol    = selStartCol;
        invalidate();
    }

    private void finishSelection(MotionEvent e) {
        selEndRow   = (int)(e.getY() / charHeight);
        selEndCol   = (int)(e.getX() / charWidth);
        isSelecting = false;
        copySelectionToClipboard();
        invalidate();
    }

    private void clearSelection() {
        selStartRow = selEndRow = -1;
        selStartCol = selEndCol = -1;
        isSelecting = false;
        invalidate();
    }

    private boolean isCellSelected(int row, int col) {
        if (selStartRow < 0 || selEndRow < 0) return false;
        int r1 = Math.min(selStartRow, selEndRow);
        int r2 = Math.max(selStartRow, selEndRow);
        int c1 = Math.min(selStartCol, selEndCol);
        int c2 = Math.max(selStartCol, selEndCol);
        if (row < r1 || row > r2) return false;
        if (row == r1 && col < c1) return false;
        if (row == r2 && col > c2) return false;
        return true;
    }

    private void copySelectionToClipboard() {
        if (selStartRow < 0 || screenChars == null) return;
        StringBuilder sb = new StringBuilder();
        int r1 = Math.min(selStartRow, selEndRow);
        int r2 = Math.max(selStartRow, selEndRow);

        for (int r = r1; r <= r2 && r < rows; r++) {
            int c1 = (r == r1) ? Math.min(selStartCol, selEndCol) : 0;
            int c2 = (r == r2) ? Math.max(selStartCol, selEndCol) : cols - 1;
            for (int c = c1; c <= c2 && c < cols; c++) {
                sb.append(screenChars[r][c]);
            }
            if (r < r2) sb.append('\n');
        }

        String text = sb.toString().trim();
        if (!text.isEmpty()) {
            ClipboardManager cm = (ClipboardManager)
                getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("terminal", text));
                Toast.makeText(getContext(), "Copied", Toast.LENGTH_SHORT).show();
            }
        }
        clearSelection();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (cursorBlink) blinkHandler.postDelayed(blinkRunnable, 500);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        blinkHandler.removeCallbacks(blinkRunnable);
    }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------
    public void setOnCellTapListener(OnCellTapListener l) { this.tapListener = l; }
    public void setCursorBlink(boolean blink) {
        this.cursorBlink = blink;
        if (!blink) cursorVisible = true;
    }
    public void setFontSize(float sp) {
        fontSize = sp;
        textPaint.setTextSize(sp * getResources().getDisplayMetrics().density);
        measureChar();
        requestLayout();
        invalidate();
    }
    public float getFontSize() { return fontSize; }
}
