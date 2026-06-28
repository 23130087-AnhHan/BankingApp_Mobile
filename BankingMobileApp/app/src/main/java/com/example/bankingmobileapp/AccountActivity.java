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
        String userId = Ui.text(userIdInput);
        if (userId.isEmpty()) {
            userIdInput.setError("Vui lòng nhập mã khách hàng");
            resultText.setText("Cần mã khách hàng để mở tài khoản.");
            return;
        }

        long parsedUserId;
        try {
            parsedUserId = Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            userIdInput.setError("Mã khách hàng không hợp lệ");
            return;
        }

        AppSession.saveUserId(this, userId);
        AccountRequest request = new AccountRequest(
                Ui.text(accountTypeInput),
                BigDecimal.ZERO,
                parsedUserId
        );
        Ui.runCall("Open account", resultText, ApiClient.getApi().createAccount(request));
    }

    private void findAccount() {
        String userIdValue = Ui.text(userIdInput);
        if (userIdValue.isEmpty()) {
            userIdInput.setError("Vui lòng nhập mã khách hàng");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(userIdValue);
        } catch (NumberFormatException ex) {
            userIdInput.setError("Mã khách hàng không hợp lệ");
            return;
        }

        AppSession.saveUserId(this, userIdValue);
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
        String accountNumber = Ui.text(accountNumberInput);
        if (accountNumber.isEmpty()) {
            accountNumberInput.setError("Vui lòng nhập số tài khoản");
            return;
        }
        AppSession.saveAccountNumber(this, accountNumber);
        Ui.runCall(
                "Activate account",
                resultText,
                ApiClient.getApi().activateAccount(accountNumber, new AccountStatusRequest("ACTIVE"))
        );
    }

    private void checkBalance() {
        String accountNumber = Ui.text(accountNumberInput);
        if (accountNumber.isEmpty()) {
            accountNumberInput.setError("Vui lòng nhập số tài khoản");
            return;
        }
        AppSession.saveAccountNumber(this, accountNumber);
        Ui.runCall("Check balance", resultText, ApiClient.getApi().getBalance(accountNumber));
    }
}
