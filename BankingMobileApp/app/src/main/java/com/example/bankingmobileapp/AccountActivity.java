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

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends Activity {
    private static final String TAG = "AccountActivity";

    private TextView resultText;
    private Button refreshButton;
    private Button transferButton;
    private Button historyButton;
    private Button pinButton;
    private Button backToDashboardButton;
    private boolean provisioningAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }

        setContentView(R.layout.activity_account);

        resultText = findViewById(R.id.resultText);
        refreshButton = findViewById(R.id.refreshAccountButton);
        transferButton = findViewById(R.id.transferButton);
        historyButton = findViewById(R.id.historyButton);
        pinButton = findViewById(R.id.pinButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        refreshButton.setOnClickListener(v -> loadPaymentAccount());
        transferButton.setOnClickListener(v -> Ui.open(this, TransferActivity.class));
        historyButton.setOnClickListener(v -> Ui.open(this, HistoryActivity.class));
        pinButton.setOnClickListener(v -> Ui.open(this, PinActivity.class));
        backToDashboardButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPaymentAccount();
    }

    private void loadPaymentAccount() {
        Long userId = currentUserId();
        if (userId == null) {
            resultText.setText("Phiên đăng nhập chưa có mã khách hàng. Vui lòng đăng nhập lại.");
            setBusy(false);
            setAccountActionsEnabled(false);
            return;
        }

        resultText.setText("Đang tải tài khoản thanh toán...");
        setBusy(true);
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    if (response.code() == 404 || response.code() == 400) {
                        AppSession.clearAccount(AccountActivity.this);
                        provisionPaymentAccount(userId);
                        return;
                    }
                    setBusy(false);
                    setAccountActionsEnabled(false);
                    resultText.setText(ApiErrorUtils.httpError(TAG, response,
                            "Không thể tải tài khoản thanh toán."));
                    return;
                }
                setBusy(false);
                saveAndRenderAccount(response.body());
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                setBusy(false);
                setAccountActionsEnabled(AppSession.hasAccount(AccountActivity.this));
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void provisionPaymentAccount(long userId) {
        if (provisioningAccount) {
            return;
        }

        provisioningAccount = true;
        resultText.setText("Đang cấp tài khoản thanh toán...");
        setBusy(true);
        AccountRequest request = new AccountRequest("PAYMENT_ACCOUNT", BigDecimal.ZERO, userId);
        ApiClient.getApi().createAccount(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                provisioningAccount = false;
                if (!response.isSuccessful() && response.code() != 409) {
                    setBusy(false);
                    setAccountActionsEnabled(false);
                    resultText.setText(ApiErrorUtils.httpError(TAG, response,
                            "Không thể cấp tài khoản thanh toán."));
                    return;
                }
                loadPaymentAccount();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                provisioningAccount = false;
                setBusy(false);
                setAccountActionsEnabled(false);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void saveAndRenderAccount(AccountResponse account) {
        if (!isPaymentAccount(account)) {
            AppSession.clearAccount(this);
            setAccountActionsEnabled(false);
            resultText.setText("Tài khoản hiện tại không phải tài khoản thanh toán. Vui lòng làm mới dashboard để cấp tài khoản chuẩn.");
            return;
        }

        String accountNumber = account.accountNumber == null ? "" : account.accountNumber.trim();
        if (accountNumber.isEmpty()) {
            setAccountActionsEnabled(false);
            resultText.setText("Backend chưa trả về số tài khoản thanh toán.");
            return;
        }

        AppSession.saveAccount(this, account);
        setAccountActionsEnabled(true);
        String balance = account.availableBalance == null ? "0" : account.availableBalance.toPlainString();

        resultText.setText("Tài khoản thanh toán"
                + "\nSố tài khoản: " + accountNumber
                + "\nTrạng thái: " + displayAccountStatus(account.accountStatus)
                + "\nSố dư khả dụng: " + balance + " đ"
                + "\n\nTài khoản này dùng để nhận tiền, chuyển tiền và xem lịch sử giao dịch.");
    }

    private void setBusy(boolean busy) {
        refreshButton.setEnabled(!busy);
        backToDashboardButton.setEnabled(!busy);
        if (busy) {
            setAccountActionsEnabled(false);
        }
    }

    private void setAccountActionsEnabled(boolean enabled) {
        transferButton.setEnabled(enabled);
        historyButton.setEnabled(enabled);
        pinButton.setEnabled(enabled);
    }

    private boolean isPaymentAccount(AccountResponse account) {
        return account != null && "PAYMENT_ACCOUNT".equalsIgnoreCase(account.accountType);
    }

    private Long currentUserId() {
        try {
            return Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String displayAccountStatus(String status) {
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
}
