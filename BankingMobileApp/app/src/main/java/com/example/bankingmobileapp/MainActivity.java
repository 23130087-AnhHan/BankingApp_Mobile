package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
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
    private Button setupAccountButton;
    private Button refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accountNumberText = findViewById(R.id.accountNumberText);
        balanceText = findViewById(R.id.balanceText);
        statusText = findViewById(R.id.statusText);
        userIdText = findViewById(R.id.userIdText);
        setupAccountButton = findViewById(R.id.setupAccountButton);
        refreshButton = findViewById(R.id.refreshButton);

        findViewById(R.id.registerTile).setOnClickListener(v -> Ui.open(this, RegisterActivity.class));
        findViewById(R.id.accountTile).setOnClickListener(v -> Ui.open(this, AccountActivity.class));
        findViewById(R.id.depositTile).setOnClickListener(v -> Ui.open(this, TransactionActivity.class));
        findViewById(R.id.transferTile).setOnClickListener(v -> Ui.open(this, TransferActivity.class));
        findViewById(R.id.historyTile).setOnClickListener(v -> Ui.open(this, HistoryActivity.class));
        refreshButton.setOnClickListener(v -> refreshAccount());
        setupAccountButton.setOnClickListener(v -> Ui.open(this, AccountActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAccount();
    }

    private void refreshAccount() {
        renderSessionSnapshot();
        if (!AppSession.hasUser(this)) {
            userIdText.setText(AppSession.getUserEmail(this).isEmpty()
                    ? "Chưa có khách hàng"
                    : AppSession.getUserEmail(this));
            statusText.setText(AppSession.hasAccount(this) ? "Tài khoản đã lưu" : "Chưa thiết lập");
            setupAccountButton.setVisibility(AppSession.hasAccount(this) ? View.GONE : View.VISIBLE);
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException ex) {
            userIdText.setText("Mã khách hàng không hợp lệ");
            statusText.setText("Cần thiết lập lại");
            setupAccountButton.setVisibility(View.VISIBLE);
            return;
        }

        userIdText.setText(AppSession.getUserEmail(this).isEmpty()
                ? "Khách hàng #" + userId
                : AppSession.getUserEmail(this));
        statusText.setText("Đang đồng bộ");
        refreshButton.setEnabled(false);
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                refreshButton.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    if (response.code() == 404) {
                        ApiErrorUtils.httpError(TAG, response, "Không tìm thấy tài khoản.");
                        AppSession.clearAccount(MainActivity.this);
                        balanceText.setText("0 ₫");
                        accountNumberText.setText("Bạn chưa có tài khoản");
                        statusText.setText("Chưa có tài khoản");
                    } else {
                        if (!response.isSuccessful()) {
                            ApiErrorUtils.httpError(TAG, response, "Không thể đồng bộ tài khoản.");
                        }
                        statusText.setText("Không thể đồng bộ");
                    }
                    setupAccountButton.setVisibility(View.VISIBLE);
                    return;
                }
                saveAndRenderAccount(response.body());
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                refreshButton.setEnabled(true);
                ApiErrorUtils.networkError(TAG, throwable);
                statusText.setText(AppSession.hasAccount(MainActivity.this)
                        ? "Offline • dữ liệu đã lưu"
                        : "Mất kết nối");
                setupAccountButton.setVisibility(AppSession.hasAccount(MainActivity.this) ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void renderSessionSnapshot() {
        String accountNumber = AppSession.getAccountNumber(this);
        String balance = AppSession.getAccountBalance(this);
        accountNumberText.setText(accountNumber.isEmpty() ? "Bạn chưa có tài khoản" : "STK  •  " + accountNumber);
        balanceText.setText((balance.isEmpty() ? "0" : balance) + " ₫");
        setupAccountButton.setVisibility(accountNumber.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveAndRenderAccount(AccountResponse account) {
        String accountNumber = account.accountNumber == null ? "" : account.accountNumber.trim();
        if (accountNumber.isEmpty()) {
            accountNumberText.setText("Bạn chưa có tài khoản");
            balanceText.setText("0 ₫");
            statusText.setText("Thiếu số tài khoản");
            setupAccountButton.setVisibility(View.VISIBLE);
            return;
        }

        AppSession.saveAccountNumber(this, accountNumber);
        if (account.accountId != null) {
            AppSession.saveAccountId(this, String.valueOf(account.accountId));
        }
        if (account.userId != null) {
            AppSession.saveUserId(this, String.valueOf(account.userId));
        }
        String balance = account.availableBalance == null ? "0" : account.availableBalance.toPlainString();
        AppSession.saveAccountBalance(this, balance);

        accountNumberText.setText("STK  •  " + accountNumber);
        balanceText.setText(balance + " ₫");
        statusText.setText(account.accountStatus == null ? "Không rõ trạng thái" : account.accountStatus);
        setupAccountButton.setVisibility(View.GONE);
    }
}
