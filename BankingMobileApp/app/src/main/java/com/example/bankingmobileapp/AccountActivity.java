package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.model.AccountRequest;
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.AccountStatusRequest;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends Activity {
    private EditText userIdInput;
    private EditText accountTypeInput;
    private EditText accountNumberInput;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        userIdInput = findViewById(R.id.userIdInput);
        accountTypeInput = findViewById(R.id.accountTypeInput);
        accountNumberInput = findViewById(R.id.accountNumberInput);
        resultText = findViewById(R.id.resultText);

        userIdInput.setText(AppSession.getUserId(this));
        accountTypeInput.setText("SAVINGS_ACCOUNT");
        accountNumberInput.setText(AppSession.getAccountNumber(this));

        findViewById(R.id.createAccountButton).setOnClickListener(v -> createAccount());
        findViewById(R.id.findAccountButton).setOnClickListener(v -> findAccount());
        findViewById(R.id.activateButton).setOnClickListener(v -> activateAccount());
        findViewById(R.id.balanceButton).setOnClickListener(v -> checkBalance());
    }

    private void createAccount() {
        AppSession.saveUserId(this, Ui.text(userIdInput));
        AccountRequest request = new AccountRequest(
                Ui.text(accountTypeInput),
                BigDecimal.ZERO,
                Long.parseLong(Ui.text(userIdInput))
        );
        Ui.runCall("Open account", resultText, ApiClient.getApi().createAccount(request));
    }

    private void findAccount() {
        AppSession.saveUserId(this, Ui.text(userIdInput));
        long userId = Long.parseLong(Ui.text(userIdInput));
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    resultText.setText("Find account failed. HTTP " + response.code());
                    return;
                }
                AccountResponse account = response.body();
                accountNumberInput.setText(account.accountNumber);
                AppSession.saveAccountNumber(AccountActivity.this, account.accountNumber);
                resultText.setText("Account found\nNumber: " + account.accountNumber
                        + "\nType: " + account.accountType
                        + "\nStatus: " + account.accountStatus
                        + "\nBalance: " + account.availableBalance);
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                resultText.setText("Find account failed: " + throwable.getMessage());
            }
        });
    }

    private void activateAccount() {
        AppSession.saveAccountNumber(this, Ui.text(accountNumberInput));
        Ui.runCall(
                "Activate account",
                resultText,
                ApiClient.getApi().activateAccount(Ui.text(accountNumberInput), new AccountStatusRequest("ACTIVE"))
        );
    }

    private void checkBalance() {
        AppSession.saveAccountNumber(this, Ui.text(accountNumberInput));
        Ui.runCall("Check balance", resultText, ApiClient.getApi().getBalance(Ui.text(accountNumberInput)));
    }
}
