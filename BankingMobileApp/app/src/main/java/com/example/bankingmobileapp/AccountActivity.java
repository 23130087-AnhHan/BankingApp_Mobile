package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.AccountRequest;
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.AccountStatusRequest;
import com.example.bankingmobileapp.model.ApiResponse;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends Activity {
    private static final String TAG = "AccountActivity";

    private EditText userIdInput;
    private EditText accountTypeInput;
    private EditText accountNumberInput;
    private TextView resultText;
    private Button createAccountButton;
    private Button findAccountButton;
    private Button activateButton;
    private Button balanceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        userIdInput = findViewById(R.id.userIdInput);
        accountTypeInput = findViewById(R.id.accountTypeInput);
        accountNumberInput = findViewById(R.id.accountNumberInput);
        resultText = findViewById(R.id.resultText);
        createAccountButton = findViewById(R.id.createAccountButton);
        findAccountButton = findViewById(R.id.findAccountButton);
        activateButton = findViewById(R.id.activateButton);
        balanceButton = findViewById(R.id.balanceButton);

        accountTypeInput.setText("SAVINGS_ACCOUNT");
        if (AppSession.hasUser(this)) {
            userIdInput.setText(AppSession.getUserId(this));
        }
        if (AppSession.hasAccount(this)) {
            accountNumberInput.setText(AppSession.getAccountNumber(this));
            resultText.setText("Đang sử dụng tài khoản mặc định đã lưu trên thiết bị.");
        } else if (!AppSession.hasUser(this)) {
            resultText.setText("Chưa có mã khách hàng. Bạn có thể nhập User ID một lần để mở hoặc tìm tài khoản.");
        }

        createAccountButton.setOnClickListener(v -> createAccount());
        findAccountButton.setOnClickListener(v -> findAccount());
        activateButton.setOnClickListener(v -> activateAccount());
        balanceButton.setOnClickListener(v -> checkBalance());
    }

    private void createAccount() {
        Long userId = readAndSaveUserId();
        if (userId == null) {
            return;
        }

        String accountType = Ui.text(accountTypeInput);
        if (accountType.isEmpty()) {
            accountTypeInput.setError("Vui lòng nhập loại tài khoản");
            return;
        }

        resultText.setText("Đang mở tài khoản...");
        createAccountButton.setEnabled(false);
        AccountRequest request = new AccountRequest(accountType, BigDecimal.ZERO, userId);
        ApiClient.getApi().createAccount(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                createAccountButton.setEnabled(true);
                if (!response.isSuccessful()) {
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể mở tài khoản."));
                    return;
                }
                String success = "Mở tài khoản thành công.\n" + Ui.formatBody(response.body());
                loadAccountByUserId(userId, success);
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                createAccountButton.setEnabled(true);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void findAccount() {
        Long userId = readAndSaveUserId();
        if (userId != null) {
            loadAccountByUserId(userId, null, findAccountButton);
        }
    }

    private Long readAndSaveUserId() {
        String userIdValue = Ui.text(userIdInput);
        if (userIdValue.isEmpty()) {
            userIdInput.setError("Vui lòng nhập mã khách hàng");
            resultText.setText("Cần mã khách hàng để tiếp tục.");
            return null;
        }

        try {
            long userId = Long.parseLong(userIdValue);
            AppSession.saveUserId(this, userIdValue);
            return userId;
        } catch (NumberFormatException ex) {
            userIdInput.setError("Mã khách hàng không hợp lệ");
            return null;
        }
    }

    private void loadAccountByUserId(long userId, String successPrefix) {
        loadAccountByUserId(userId, successPrefix, null);
    }

    private void loadAccountByUserId(long userId, String successPrefix, Button actionButton) {
        if (successPrefix == null) {
            resultText.setText("Đang tìm tài khoản...");
        }
        if (actionButton != null) {
            actionButton.setEnabled(false);
        }
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (actionButton != null) {
                    actionButton.setEnabled(true);
                }
                if (!response.isSuccessful() || response.body() == null) {
                    String message = response.isSuccessful()
                            ? "Backend phản hồi thành công nhưng không có dữ liệu tài khoản."
                            : ApiErrorUtils.httpError(TAG, response, "Chưa tìm thấy tài khoản cho khách hàng này.");
                    resultText.setText(successPrefix == null ? message : successPrefix + "\n" + message);
                    return;
                }
                saveAndRenderAccount(response.body(), successPrefix);
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                if (actionButton != null) {
                    actionButton.setEnabled(true);
                }
                String message = ApiErrorUtils.networkError(TAG, throwable);
                resultText.setText(successPrefix == null ? message : successPrefix + "\n" + message);
            }
        });
    }

    private void saveAndRenderAccount(AccountResponse account, String successPrefix) {
        String accountNumber = account.accountNumber == null ? "" : account.accountNumber.trim();
        if (accountNumber.isEmpty()) {
            resultText.setText("Backend đã phản hồi nhưng chưa có số tài khoản.");
            return;
        }

        AppSession.saveAccountNumber(this, accountNumber);
        if (account.accountId != null) {
            AppSession.saveAccountId(this, String.valueOf(account.accountId));
        }
        if (account.userId != null) {
            AppSession.saveUserId(this, String.valueOf(account.userId));
            userIdInput.setText(String.valueOf(account.userId));
        }
        if (account.availableBalance != null) {
            AppSession.saveAccountBalance(this, account.availableBalance.toPlainString());
        }

        accountNumberInput.setText(accountNumber);
        String details = "Đã lưu tài khoản mặc định\n"
                + "Số tài khoản: " + accountNumber
                + "\nLoại: " + valueOrUnknown(account.accountType)
                + "\nTrạng thái: " + valueOrUnknown(account.accountStatus)
                + "\nSố dư: " + (account.availableBalance == null ? "0" : account.availableBalance.toPlainString()) + " ₫";
        resultText.setText(successPrefix == null ? details : successPrefix + "\n\n" + details);
    }

    private void activateAccount() {
        String accountNumber = Ui.text(accountNumberInput);
        if (accountNumber.isEmpty()) {
            accountNumberInput.setError("Vui lòng nhập số tài khoản");
            return;
        }

        AppSession.saveAccountNumber(this, accountNumber);
        resultText.setText("Đang kích hoạt tài khoản...");
        activateButton.setEnabled(false);
        ApiClient.getApi().activateAccount(accountNumber, new AccountStatusRequest("ACTIVE"))
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        activateButton.setEnabled(true);
                        if (!response.isSuccessful()) {
                            resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể kích hoạt tài khoản."));
                            return;
                        }
                        String success = "Kích hoạt tài khoản thành công.\n" + Ui.formatBody(response.body());
                        if (AppSession.hasUser(AccountActivity.this)) {
                            try {
                                loadAccountByUserId(Long.parseLong(AppSession.getUserId(AccountActivity.this)), success);
                            } catch (NumberFormatException ex) {
                                resultText.setText(success);
                            }
                        } else {
                            resultText.setText(success);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                        activateButton.setEnabled(true);
                        resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
                    }
                });
    }

    private void checkBalance() {
        String accountNumber = Ui.text(accountNumberInput);
        if (accountNumber.isEmpty()) {
            accountNumberInput.setError("Vui lòng nhập số tài khoản");
            return;
        }

        AppSession.saveAccountNumber(this, accountNumber);
        resultText.setText("Đang tải số dư...");
        balanceButton.setEnabled(false);
        ApiClient.getApi().getBalance(accountNumber).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                balanceButton.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    resultText.setText(response.isSuccessful()
                            ? "Backend chưa trả về số dư."
                            : ApiErrorUtils.httpError(TAG, response, "Không thể tải số dư."));
                    return;
                }
                AppSession.saveAccountBalance(AccountActivity.this, response.body());
                resultText.setText("Số dư khả dụng\n" + response.body() + " ₫\n\nĐã cập nhật tài khoản mặc định.");
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                balanceButton.setEnabled(true);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private String valueOrUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "Chưa có" : value;
    }
}
