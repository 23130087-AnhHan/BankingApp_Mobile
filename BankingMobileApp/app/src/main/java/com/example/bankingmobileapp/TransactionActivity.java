package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.model.TransactionRequest;

import java.math.BigDecimal;

public class TransactionActivity extends Activity {
    private EditText accountNumberInput;
    private EditText transactionTypeInput;
    private EditText amountInput;
    private EditText descriptionInput;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        accountNumberInput = findViewById(R.id.accountNumberInput);
        transactionTypeInput = findViewById(R.id.transactionTypeInput);
        amountInput = findViewById(R.id.amountInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        resultText = findViewById(R.id.resultText);

        accountNumberInput.setText(AppSession.getAccountNumber(this));
        transactionTypeInput.setText("DEPOSIT");

        findViewById(R.id.depositButton).setOnClickListener(v -> submit("DEPOSIT"));
        findViewById(R.id.withdrawButton).setOnClickListener(v -> submit("WITHDRAWAL"));
    }

    private void submit(String type) {
        String accountNumber = Ui.text(accountNumberInput);
        String amountValue = Ui.text(amountInput);
        if (accountNumber.isEmpty()) {
            accountNumberInput.setError("Vui lòng nhập số tài khoản");
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

        transactionTypeInput.setText(type);
        AppSession.saveAccountNumber(this, accountNumber);
        TransactionRequest request = new TransactionRequest(
                accountNumber,
                type,
                amount,
                Ui.text(descriptionInput)
        );
        Ui.runCall(type.equals("DEPOSIT") ? "Nạp tiền" : "Rút tiền", resultText, ApiClient.getApi().createTransaction(request));
    }
}
