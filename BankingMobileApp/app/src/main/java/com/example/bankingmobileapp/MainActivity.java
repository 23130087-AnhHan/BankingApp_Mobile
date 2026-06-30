package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.RefreshTokenRequest;

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
        if (!AppSession.hasValidSession(this)) {
            openLoginAndFinish();
            return;
        }

        setContentView(R.layout.activity_main);

        accountNumberText = findViewById(R.id.accountNumberText);
        balanceText = findViewById(R.id.balanceText);
        statusText = findViewById(R.id.statusText);
        userIdText = findViewById(R.id.userIdText);
        setupAccountButton = findViewById(R.id.setupAccountButton);
        refreshButton = findViewById(R.id.refreshButton);

        findViewById(R.id.accountTile).setOnClickListener(v -> Ui.open(this, AccountActivity.class));
        findViewById(R.id.depositTile).setOnClickListener(v -> Ui.open(this, TransactionActivity.class));
        findViewById(R.id.transferTile).setOnClickListener(v -> Ui.open(this, TransferActivity.class));
        findViewById(R.id.historyTile).setOnClickListener(v -> Ui.open(this, HistoryActivity.class));
        findViewById(R.id.logoutButton).setOnClickListener(v -> logout());
        refreshButton.setOnClickListener(v -> refreshAccount());
        setupAccountButton.setOnClickListener(v -> Ui.open(this, AccountActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppSession.hasValidSession(this)) {
            openLoginAndFinish();
        } else {
            refreshAccount();
        }
    }

    private void refreshAccount() {
        renderSessionSnapshot();
        if (!AppSession.hasUser(this)) {
            userIdText.setText(AppSession.getUserEmail(this).isEmpty()
                    ? "Chưa có khách hàng"
                    : AppSession.getUserEmail(this));
            statusText.setText(AppSession.hasAccount(this) ? "Đã có tài khoản" : "Chưa có tài khoản");
            setupAccountButton.setVisibility(AppSession.hasAccount(this) ? View.GONE : View.VISIBLE);
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException ex) {
            userIdText.setText("Phiên đăng nhập chưa có mã khách hàng");
            statusText.setText("Cần đăng nhập lại");
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
                    if (response.code() == 404 || response.code() == 400) {
                        ApiErrorUtils.httpError(TAG, response, "Không tìm thấy tài khoản.");
                        AppSession.clearAccount(MainActivity.this);
                        balanceText.setText("0 VND");
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
                        ? "Offline - dữ liệu đã lưu"
                        : "Mất kết nối");
                setupAccountButton.setVisibility(AppSession.hasAccount(MainActivity.this) ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void renderSessionSnapshot() {
        String accountNumber = AppSession.getAccountNumber(this);
        String balance = AppSession.getAccountBalance(this);
        accountNumberText.setText(accountNumber.isEmpty() ? "Bạn chưa có tài khoản" : "STK  •  " + accountNumber);
        balanceText.setText(CurrencyUtils.formatVnd(balance));
        setupAccountButton.setVisibility(accountNumber.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveAndRenderAccount(AccountResponse account) {
        String accountNumber = account.accountNumber == null ? "" : account.accountNumber.trim();
        if (accountNumber.isEmpty()) {
            accountNumberText.setText("Bạn chưa có tài khoản");
            balanceText.setText("0 VND");
            statusText.setText("Thiếu số tài khoản");
            setupAccountButton.setVisibility(View.VISIBLE);
            return;
        }

        AppSession.saveAccount(this, account);
        String balance = account.availableBalance == null ? "0" : account.availableBalance.toPlainString();

        accountNumberText.setText("STK  •  " + accountNumber);
        balanceText.setText(CurrencyUtils.formatVnd(balance));
        statusText.setText(formatAccountStatus(account.accountStatus));
        setupAccountButton.setVisibility(View.GONE);
    }

    private String formatAccountStatus(String status) {
        if ("ACTIVE".equals(status)) {
            return "Đang hoạt động";
        }
        if ("PENDING".equals(status)) {
            return "Đang xử lý";
        }
        if ("CLOSED".equals(status)) {
            return "Đã đóng";
        }
        return status == null || status.trim().isEmpty() ? "Không rõ trạng thái" : status;
    }

    private void logout() {
        String refreshToken = AppSession.getRefreshToken(this);
        if (refreshToken.isEmpty()) {
            finishLogout();
            return;
        }
        ApiClient.getApi().logout(new RefreshTokenRequest(refreshToken)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                finishLogout();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        AppSession.clearLoginState(this);
        Ui.openAndClear(this, WelcomeActivity.class);
    }

    private void openLoginAndFinish() {
        Ui.openAndClear(this, WelcomeActivity.class);
    }
}
