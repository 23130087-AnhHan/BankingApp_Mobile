package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.math.BigDecimal;

public class TransferConfirmActivity extends Activity {
    static final String EXTRA_FROM = "transfer_from";
    static final String EXTRA_BANK = "transfer_bank";
    static final String EXTRA_TO = "transfer_to";
    static final String EXTRA_RECIPIENT = "transfer_recipient";
    static final String EXTRA_AMOUNT = "transfer_amount";
    static final String EXTRA_NOTE = "transfer_note";
    static final String EXTRA_AUTH_METHOD = "transfer_auth_method";
    static final String AUTH_PIN = "PIN";
    static final String AUTH_OTP = "OTP";

    static void putExtras(Intent intent, String from, String bank, String to,
                          String recipient, String amount, String note) {
        intent.putExtra(EXTRA_FROM, from);
        intent.putExtra(EXTRA_BANK, bank);
        intent.putExtra(EXTRA_TO, to);
        intent.putExtra(EXTRA_RECIPIENT, recipient);
        intent.putExtra(EXTRA_AMOUNT, amount);
        intent.putExtra(EXTRA_NOTE, note);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }
        setContentView(R.layout.activity_transfer_confirm);

        String from = value(EXTRA_FROM);
        String bank = value(EXTRA_BANK);
        String to = value(EXTRA_TO);
        String recipient = value(EXTRA_RECIPIENT);
        String amount = value(EXTRA_AMOUNT);
        String note = value(EXTRA_NOTE);

        ((TextView) findViewById(R.id.confirmSummaryText)).setText(
                "Hình thức chuyển: Chuyển tiền nhanh 24/7"
                        + "\nTài khoản nguồn: " + from
                        + "\nTài khoản nhận: " + to
                        + "\nTên người nhận: " + recipient
                        + "\nNgân hàng nhận: " + bank
                        + "\nNội dung: " + (note.isEmpty() ? "Không có" : note)
                        + "\nPhí chuyển tiền: Miễn phí"
                        + "\nSố tiền: " + CurrencyUtils.formatVnd(new BigDecimal(amount)));

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.confirmButton).setOnClickListener(v -> {
            RadioGroup group = findViewById(R.id.authMethodGroup);
            String method = group.getCheckedRadioButtonId() == R.id.otpEmailOption ? AUTH_OTP : AUTH_PIN;
            Intent intent = new Intent(this, TransferAuthActivity.class);
            intent.putExtras(getIntent());
            intent.putExtra(EXTRA_AUTH_METHOD, method);
            startActivity(intent);
        });
    }

    private String value(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value.trim();
    }
}
