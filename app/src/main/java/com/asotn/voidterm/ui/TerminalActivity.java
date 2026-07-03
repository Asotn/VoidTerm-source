/*
 * VoidTerm - TerminalActivity
 * The main (launcher) screen. Hosts the terminal output view and the
 * command input field, wires them to TerminalSession + CommandProcessor.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */
package com.asotn.voidterm.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.asotn.voidterm.R;
import com.asotn.voidterm.service.BootstrapService;
import com.asotn.voidterm.terminal.CommandProcessor;
import com.asotn.voidterm.terminal.TerminalSession;
import com.asotn.voidterm.utils.DistroCatalog;
import com.asotn.voidterm.utils.EnvironmentManager;

public class TerminalActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATIONS = 5001;

    private TextView    outputView;
    private ScrollView  scrollView;
    private EditText    inputView;
    private TerminalSession session;

    /** True while we're waiting for the user to type a distro number. */
    private boolean awaitingDistroChoice = false;

    private final BroadcastReceiver bootstrapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra(BootstrapService.EXTRA_STATUS);
            switch (intent.getAction() == null ? "" : intent.getAction()) {
                case BootstrapService.BROADCAST_PROGRESS:
                    if (status != null) appendOutput(status + "\r\n");
                    break;
                case BootstrapService.BROADCAST_ERROR:
                    appendOutput("\r\n[!] " + status + "\r\n" +
                            "Check your internet connection and try again:\r\n\r\n" +
                            DistroCatalog.renderMenu());
                    awaitingDistroChoice = true;
                    break;
                case BootstrapService.BROADCAST_DONE:
                    appendOutput("\r\nStarting shell...\r\n\r\n");
                    session.start();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        outputView = findViewById(R.id.text_output);
        scrollView = findViewById(R.id.scroll_output);
        inputView  = findViewById(R.id.edit_input);
        ImageButton sendBtn = findViewById(R.id.btn_send);

        requestNotificationPermissionIfNeeded();

        session = new TerminalSession(this);
        session.setOutputCallback(this::appendOutput);

        IntentFilter bootstrapFilter = new IntentFilter();
        bootstrapFilter.addAction(BootstrapService.BROADCAST_PROGRESS);
        bootstrapFilter.addAction(BootstrapService.BROADCAST_DONE);
        bootstrapFilter.addAction(BootstrapService.BROADCAST_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(bootstrapReceiver, bootstrapFilter);

        appendOutput("VoidTerm - Kali Linux Terminal for Android\r\n" +
                "Type voidterm-help for usage.\r\n\r\n");

        if (EnvironmentManager.isBootstrapped()) {
            String name = EnvironmentManager.getInstalledDistroName();
            if (name != null) appendOutput("Environment: " + name + "\r\n\r\n");
            session.start();
        } else {
            appendOutput(DistroCatalog.renderMenu());
            awaitingDistroChoice = true;
        }

        sendBtn.setOnClickListener(v -> submitInput());
        inputView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitInput();
                return true;
            }
            return false;
        });
    }

    private void startDistroInstall(DistroCatalog.Distro distro) {
        awaitingDistroChoice = false;
        appendOutput("\r\nInstalling " + distro.displayName + "...\r\n" +
                "This downloads a real Linux filesystem — it can take a while\r\n" +
                "and needs a stable connection (roughly 100-400 MB).\r\n\r\n");

        Intent intent = new Intent(this, BootstrapService.class);
        intent.putExtra(BootstrapService.EXTRA_DISTRO_ID, distro.id);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    private void submitInput() {
        String text = inputView.getText().toString();
        inputView.setText("");
        if (text.isEmpty()) return;

        appendOutput("$ " + text + "\r\n");

        if (awaitingDistroChoice) {
            handleDistroChoice(text.trim());
            return;
        }

        CommandProcessor.CommandResult result = CommandProcessor.process(text, this);
        switch (result.type) {
            case CLEAR:
                outputView.setText("");
                break;
            case EXIT:
                finish();
                break;
            case SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case HELP:
            case ABOUT:
                appendOutput(result.output);
                break;
            case FILE_PERMISSION:
                startActivity(new Intent(this, PackageInstallActivity.class));
                break;
            case PASSTHROUGH:
            default:
                if (result.passthroughCmd != null) {
                    session.sendInput(result.passthroughCmd + "\n");
                }
                break;
        }
    }

    private void handleDistroChoice(String input) {
        int n;
        try {
            n = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            appendOutput("Please type a number from the list above.\r\n\r\n> ");
            return;
        }
        DistroCatalog.Distro distro = DistroCatalog.byNumber(n);
        if (distro == null) {
            appendOutput("No distro with that number. Try again.\r\n\r\n> ");
            return;
        }
        startDistroInstall(distro);
    }

    private void appendOutput(String text) {
        if (text == null) return;
        runOnUiThread(() -> {
            outputView.append(text);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.terminal_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_ctrl_c) {
            session.sendSignalInterrupt();
            return true;
        } else if (id == R.id.action_clear) {
            outputView.setText("");
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bootstrapReceiver);
        if (session != null) session.stop();
    }
}
