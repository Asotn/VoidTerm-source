/*
 * VoidTerm - AboutActivity
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.asotn.voidterm.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("About VoidTerm");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button btnGithub = findViewById(R.id.btn_github);
        if (btnGithub != null) {
            btnGithub.setOnClickListener(v -> {
                startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/Asotn")));
            });
        }

        Button btnEmail = findViewById(R.id.btn_email);
        if (btnEmail != null) {
            btnEmail.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:s.pi@outlook.sa"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "VoidTerm Feedback");
                startActivity(Intent.createChooser(intent, "Send Email"));
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
