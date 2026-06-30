package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
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

    private RadioGroup accountTypeGroup;
    private TextView resultText;
    private Button createAccountButton;
    private Button backToDashboardButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }

        setContentView(R.layout.activity_account);

        accountTypeGroup = findViewById(R.id.accountTypeGroup);
        resultText = findViewById(R.id.resultText);
        createAccountButton = findViewById(R.id.createAccountButton);
        backToDashboardButton = findViewById(R.id.backToDashboardButton);

        createAccountButton.setOnClickListener(v -> createAccount());
        backToDashboardButton.setOnClickListener(v -> finish());

        if (AppSession.hasAccount(this)) {
            resultText.setText("Bạn đã có tài khoản mặc định.\nSố tài khoản: "
                    + AppSession.getAccountNumber(this)
                    + "\nSố dư: " + balanceOrZero() + " đ");
            createAccountButton.setEnabled(false);
        }
    }

    private void createAccount() {
        Long userId = currentUserId();
        if (userId == null) {
            resultText.setText("Phiên đăng nhập chưa có mã khách hàng. Vui lòng đăng nhập lại.");
            return;
        }

        resultText.setText("Đang mở tài khoản...");
        createAccountButton.setEnabled(false);
        AccountRequest request = new AccountRequest(selectedBackendAccountType(), BigDecimal.ZERO, userId);
        ApiClient.getApi().createAccount(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (!response.isSuccessful()) {
                    if (response.code() == 409) {
                        loadAccountByUserId(userId, "Bạn đã có tài khoản. Đang cập nhật dashboard...");
                        return;
                    }
                    createAccountButton.setEnabled(true);
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể mở tài khoản."));
                    return;
                }
                loadAccountByUserId(userId, "Mở tài khoản thành công.");
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                createAccountButton.setEnabled(true);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void loadAccountByUserId(long userId, String successPrefix) {
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                createAccountButton.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    String message = response.isSuccessful()
                            ? "Backend đã tạo tài khoản nhưng chưa trả về dữ liệu tài khoản."
                            : ApiErrorUtils.httpError(TAG, response, "Không thể tải tài khoản vừa mở.");
                    resultText.setText(successPrefix + "\n" + message);
                    return;
                }
                saveAndRenderAccount(response.body(), successPrefix);
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                createAccountButton.setEnabled(true);
                resultText.setText(successPrefix + "\n" + ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void saveAndRenderAccount(AccountResponse account, String successPrefix) {
        String accountNumber = account.accountNumber == null ? "" : account.accountNumber.trim();
        if (accountNumber.isEmpty()) {
            resultText.setText(successPrefix + "\nBackend chưa trả về số tài khoản.");
            return;
        }

        AppSession.saveAccount(this, account);
        String balance = account.availableBalance == null ? "0" : account.availableBalance.toPlainString();
        resultText.setText(successPrefix
                + "\nSố tài khoản: " + accountNumber
                + "\nLoại tài khoản: " + displayAccountType(account.accountType)
                + "\nTrạng thái: " + displayAccountStatus(account.accountStatus)
                + "\nSố dư: " + balance + " đ");
    }

    private Long currentUserId() {
        try {
            return Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String selectedBackendAccountType() {
        if (accountTypeGroup.getCheckedRadioButtonId() == R.id.savingsAccountOption) {
            return "FIXED_DEPOSIT";
        }
        return "SAVINGS_ACCOUNT";
    }

    private String displayAccountType(String type) {
        if ("FIXED_DEPOSIT".equals(type)) {
            return "Tài khoản tiết kiệm";
        }
        return "Tài khoản thanh toán";
    }

    private String displayAccountStatus(String status) {
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

    private String balanceOrZero() {
        String balance = AppSession.getAccountBalance(this);
        return balance.isEmpty() ? "0" : balance;
    }
}
