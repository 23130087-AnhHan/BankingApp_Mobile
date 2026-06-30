package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class WelcomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSession.clearLoginState(this);

        if (AppSession.hasRememberedUser(this)) {
            Ui.openAndClear(this, QuickLoginActivity.class);
            return;
        }

        setContentView(R.layout.activity_welcome);

        findViewById(R.id.loginButton).setOnClickListener(v -> Ui.open(this, LoginActivity.class));
        findViewById(R.id.openAccountButton).setOnClickListener(v -> Ui.open(this, RegisterActivity.class));
        findViewById(R.id.helpShortcut).setOnClickListener(v -> showDemoToast());
        findViewById(R.id.rateShortcut).setOnClickListener(v -> showDemoToast());
        findViewById(R.id.branchShortcut).setOnClickListener(v -> showDemoToast());
    }

    private void showDemoToast() {
        Toast.makeText(this, "Tính năng đang được phát triển", Toast.LENGTH_SHORT).show();
    }
}
