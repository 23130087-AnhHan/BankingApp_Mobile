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
        amountInput.setText("1000");
        descriptionInput.setText("Cash deposit");

        findViewById(R.id.depositButton).setOnClickListener(v -> submit("DEPOSIT"));
        findViewById(R.id.withdrawButton).setOnClickListener(v -> submit("WITHDRAWAL"));
    }

    private void submit(String type) {
        transactionTypeInput.setText(type);
        AppSession.saveAccountNumber(this, Ui.text(accountNumberInput));
        TransactionRequest request = new TransactionRequest(
                Ui.text(accountNumberInput),
                type,
                new BigDecimal(Ui.text(amountInput)),
                Ui.text(descriptionInput)
        );
        Ui.runCall(type, resultText, ApiClient.getApi().createTransaction(request));
    }
}
