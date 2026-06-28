package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.model.AccountResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private TextView accountNumberText;
    private TextView balanceText;
    private TextView statusText;
    private TextView userIdText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.isLoggedIn(this)) {
            openLoginAndFinish();
            return;
        }

        setContentView(R.layout.activity_main);

        accountNumberText = findViewById(R.id.accountNumberText);
        balanceText = findViewById(R.id.balanceText);
        statusText = findViewById(R.id.statusText);
        userIdText = findViewById(R.id.userIdText);

        findViewById(R.id.accountTile).setOnClickListener(v -> Ui.open(this, AccountActivity.class));
        findViewById(R.id.depositTile).setOnClickListener(v -> Ui.open(this, TransactionActivity.class));
        findViewById(R.id.transferTile).setOnClickListener(v -> Ui.open(this, TransferActivity.class));
        findViewById(R.id.historyTile).setOnClickListener(v -> Ui.open(this, HistoryActivity.class));
        findViewById(R.id.refreshButton).setOnClickListener(v -> refreshAccount());
        findViewById(R.id.logoutButton).setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAccount();
    }

    private void refreshAccount() {
        String userId = AppSession.getUserId(this);
        String accountNumber = AppSession.getAccountNumber(this);
        userIdText.setText("Khách hàng #" + userId);
        accountNumberText.setText(accountNumber.isEmpty() ? "Chưa chọn tài khoản" : "STK  •  " + accountNumber);
        balanceText.setText("Đang tải...");
        statusText.setText("Đang đồng bộ");

        try {
            long id = Long.parseLong(userId);
            ApiClient.getApi().getAccountByUserId(id).enqueue(new Callback<AccountResponse>() {
                @Override
                public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        balanceText.setText("0 ₫");
                        statusText.setText(response.code() == 404 ? "Chưa có tài khoản" : "Lỗi đồng bộ");
                        Log.e(TAG, "Refresh account failed. HTTP " + response.code());
                        return;
                    }
                    AccountResponse account = response.body();
                    String resolvedAccountNumber = account.accountNumber == null ? "" : account.accountNumber;
                    AppSession.saveAccount(MainActivity.this, account);
                    accountNumberText.setText(resolvedAccountNumber.isEmpty()
                            ? "Chưa có số tài khoản"
                            : "STK  •  " + resolvedAccountNumber);
                    balanceText.setText((account.availableBalance == null
                            ? "0"
                            : account.availableBalance.toPlainString()) + " ₫");
                    statusText.setText(account.accountStatus == null ? "Không rõ" : account.accountStatus);
                }

                @Override
                public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                    balanceText.setText("-- ₫");
                    statusText.setText("Mất kết nối");
                    Log.e(TAG, "Refresh account network failure", throwable);
                }
            });
        } catch (NumberFormatException ex) {
            balanceText.setText("-- ₫");
            statusText.setText("User ID không hợp lệ");
        }
    }

    private void logout() {
        AppSession.clearLoginState(this);
        Ui.openAndClear(this, WelcomeActivity.class);
    }

    private void openLoginAndFinish() {
        Ui.openAndClear(this, WelcomeActivity.class);
    }
}
