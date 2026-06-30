package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.AccountRequest;
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.RefreshTokenRequest;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private TextView accountNumberText;
    private TextView balanceText;
    private TextView statusText;
    private TextView userIdText;
    private Button refreshButton;
    private boolean provisioningAccount;

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
        refreshButton = findViewById(R.id.refreshButton);

        findViewById(R.id.accountTile).setOnClickListener(v -> Ui.open(this, AccountActivity.class));
        findViewById(R.id.transferTile).setOnClickListener(v -> Ui.open(this, TransferActivity.class));
        findViewById(R.id.historyTile).setOnClickListener(v -> Ui.open(this, HistoryActivity.class));
        findViewById(R.id.logoutButton).setOnClickListener(v -> logout());
        refreshButton.setOnClickListener(v -> refreshPaymentAccount());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppSession.hasValidSession(this)) {
            openLoginAndFinish();
        } else {
            refreshPaymentAccount();
        }
    }

    private void refreshPaymentAccount() {
        renderSessionSnapshot();
        if (!AppSession.hasUser(this)) {
            userIdText.setText(AppSession.getUserEmail(this).isEmpty()
                    ? "Chưa có khách hàng"
                    : AppSession.getUserEmail(this));
            statusText.setText(AppSession.hasAccount(this) ? "Đã có tài khoản" : "Chưa có tài khoản");
            return;
        }

        Long userId = currentUserId();
        if (userId == null) {
            userIdText.setText("Phiên đăng nhập hết hạn");
            statusText.setText("Cần đăng nhập lại");
            return;
        }

        userIdText.setText(AppSession.getUserEmail(this).isEmpty()
                ? "Khách hàng #" + userId
                : AppSession.getUserEmail(this));
        statusText.setText("Đang đồng bộ...");
        refreshButton.setEnabled(false);

        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                refreshButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    saveAndRenderAccount(response.body());
                } else if (response.code() == 404 || response.code() == 400) {
                    // Try to create if not found
                    provisionDefaultPaymentAccount(userId);
                } else {
                    statusText.setText("Không thể đồng bộ");
                    ApiErrorUtils.httpError(TAG, response, "Lỗi kết nối tài khoản.");
                }
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                refreshButton.setEnabled(true);
                statusText.setText("Offline");
                renderSessionSnapshot();
            }
        });
    }

    private void provisionDefaultPaymentAccount(long userId) {
        if (provisioningAccount) return;
        provisioningAccount = true;
        refreshButton.setEnabled(false);
        statusText.setText("Đang mở tài khoản...");

        AccountRequest request = new AccountRequest("SAVINGS_ACCOUNT", BigDecimal.ZERO, userId);
        ApiClient.getApi().createAccount(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                provisioningAccount = false;
                // If success or already exists (409), try to fetch it again
                if (response.isSuccessful() || response.code() == 409) {
                    refreshPaymentAccount();
                } else {
                    refreshButton.setEnabled(true);
                    statusText.setText("Lỗi cấp tài khoản");
                    ApiErrorUtils.httpError(TAG, response, "Không thể mở tài khoản mới.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                provisioningAccount = false;
                refreshButton.setEnabled(true);
                statusText.setText("Lỗi kết nối");
            }
        });
    }

    private void renderProvisioningState() {
        balanceText.setText("0 VND");
        accountNumberText.setText("Đang cấp tài khoản thanh toán");
        statusText.setText("Đang mở tài khoản");
    }

    private void renderSessionSnapshot() {
        String accountNumber = AppSession.getAccountNumber(this);
        String balance = AppSession.getAccountBalance(this);
        accountNumberText.setText(accountNumber.isEmpty()
                ? "Đang kiểm tra tài khoản"
                : "STK  •  " + accountNumber);
        balanceText.setText(CurrencyUtils.formatVnd(balance));
    }

    private void saveAndRenderAccount(AccountResponse account) {
        if (account == null || !AppSession.isPaymentAccount(account.accountType)) {
            AppSession.clearAccount(this);
            statusText.setText("Cần tài khoản thanh toán");
            accountNumberText.setText(account == null ? "Không tìm thấy tài khoản" : "Tài khoản hiện tại không phải tài khoản thanh toán");
            balanceText.setText("0 VND");
            return;
        }

        String accountNumber = account.accountNumber == null ? "" : account.accountNumber.trim();
        if (accountNumber.isEmpty()) {
            accountNumberText.setText("Chưa có số tài khoản");
            balanceText.setText("0 VND");
            statusText.setText("Thiếu số tài khoản");
            return;
        }

        AppSession.saveAccount(this, account);
        String balance = account.availableBalance == null ? "0" : account.availableBalance.toPlainString();

        accountNumberText.setText("STK  •  " + accountNumber);
        balanceText.setText(CurrencyUtils.formatVnd(balance));
        statusText.setText(formatAccountStatus(account.accountStatus));
    }

    private boolean isPaymentAccount(AccountResponse account) {
        return account != null && AppSession.isPaymentAccount(account.accountType);
    }

    private Long currentUserId() {
        try {
            return Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatAccountStatus(String status) {
        if ("ACTIVE".equals(status)) {
            return "Đang hoạt động";
        }
        if ("PENDING".equals(status)) {
            return "Đang chờ xử lý";
        }
        if ("BLOCKED".equals(status)) {
            return "Đang bị khóa";
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
