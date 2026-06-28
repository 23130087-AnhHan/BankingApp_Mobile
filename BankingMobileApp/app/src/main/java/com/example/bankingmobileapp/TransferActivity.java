package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.model.FundTransferRequest;

import java.math.BigDecimal;

public class TransferActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        EditText fromAccountInput = findViewById(R.id.fromAccountInput);
        EditText toAccountInput = findViewById(R.id.toAccountInput);
        EditText amountInput = findViewById(R.id.amountInput);
        TextView resultText = findViewById(R.id.resultText);

        fromAccountInput.setText(AppSession.getAccountNumber(this));
        amountInput.setText("100");

        findViewById(R.id.transferButton).setOnClickListener(v -> {
            String fromAccount = Ui.text(fromAccountInput);
            String toAccount = Ui.text(toAccountInput);
            String amountValue = Ui.text(amountInput);
            if (fromAccount.isEmpty()) {
                fromAccountInput.setError("Vui lòng nhập tài khoản nguồn");
                return;
            }
            if (toAccount.isEmpty()) {
                toAccountInput.setError("Vui lòng nhập tài khoản nhận");
                return;
            }
            if (amountValue.isEmpty()) {
                amountInput.setError("Vui lòng nhập số tiền");
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountValue);
            } catch (NumberFormatException ex) {
                amountInput.setError("Số tiền không hợp lệ");
                return;
            }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                amountInput.setError("Số tiền phải lớn hơn 0");
                return;
            }

            AppSession.saveAccountNumber(this, fromAccount);
            FundTransferRequest request = new FundTransferRequest(
                    fromAccount,
                    toAccount,
                    amount
            );
            Ui.runCall("Transfer", resultText, ApiClient.getApi().transfer(request));
        });
    }
}
