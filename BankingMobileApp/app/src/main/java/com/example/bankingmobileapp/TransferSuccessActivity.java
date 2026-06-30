package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransferSuccessActivity extends Activity {
    static final String EXTRA_REFERENCE = "transfer_reference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }
        setContentView(R.layout.activity_transfer_success);

        String amount = value(TransferConfirmActivity.EXTRA_AMOUNT);
        String to = value(TransferConfirmActivity.EXTRA_TO);
        String recipient = value(TransferConfirmActivity.EXTRA_RECIPIENT);
        String bank = value(TransferConfirmActivity.EXTRA_BANK);
        String note = value(TransferConfirmActivity.EXTRA_NOTE);
        String reference = value(EXTRA_REFERENCE);

        ((TextView) findViewById(R.id.successAmountText)).setText(CurrencyUtils.formatVnd(new BigDecimal(amount)));
        ((TextView) findViewById(R.id.successTimeText)).setText(
                new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        ((TextView) findViewById(R.id.successToAccountText)).setText("Tài khoản nhận: " + to);
        ((TextView) findViewById(R.id.successRecipientText)).setText("Tên người nhận: " + recipient);
        ((TextView) findViewById(R.id.successBankText)).setText("Ngân hàng nhận: " + bank);
        ((TextView) findViewById(R.id.successNoteText)).setText("Nội dung: " + (note.isEmpty() ? "Không có" : note));
        ((TextView) findViewById(R.id.successReferenceText)).setText("Mã giao dịch: " + (reference.isEmpty() ? "-" : reference));
        findViewById(R.id.newTransferButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private String value(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value.trim();
    }
}
