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
            AppSession.saveAccountNumber(this, Ui.text(fromAccountInput));
            FundTransferRequest request = new FundTransferRequest(
                    Ui.text(fromAccountInput),
                    Ui.text(toAccountInput),
                    new BigDecimal(Ui.text(amountInput))
            );
            Ui.runCall("Transfer", resultText, ApiClient.getApi().transfer(request));
        });
    }
}
