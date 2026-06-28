package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.AccountRequest;
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.CreateUserRequest;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends Activity {
    private static final String TAG = "RegisterActivity";

    private Button registerButton;
    private Button openAccountButton;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText firstNameInput = findViewById(R.id.firstNameInput);
        EditText lastNameInput = findViewById(R.id.lastNameInput);
        EditText phoneInput = findViewById(R.id.phoneInput);
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        EditText userIdInput = findViewById(R.id.userIdInput);
        registerButton = findViewById(R.id.registerButton);
        openAccountButton = findViewById(R.id.openAccountButton);
        resultText = findViewById(R.id.resultText);

        registerButton.setOnClickListener(v -> {
            if (!validateRequired(firstNameInput, "Vui lòng nhập tên")
                    || !validateRequired(lastNameInput, "Vui lòng nhập họ và tên đệm")
                    || !validateRequired(phoneInput, "Vui lòng nhập số điện thoại")
                    || !validateRequired(emailInput, "Vui lòng nhập email")
                    || !validateRequired(passwordInput, "Vui lòng nhập mật khẩu")) {
                return;
            }

            String email = Ui.text(emailInput);
            CreateUserRequest request = new CreateUserRequest(
                    Ui.text(firstNameInput),
                    Ui.text(lastNameInput),
                    Ui.text(phoneInput),
                    email,
                    Ui.text(passwordInput)
            );
            register(request, email);
        });
        openAccountButton.setOnClickListener(v -> createAccount(userIdInput));
        findViewById(R.id.loginButton).setOnClickListener(v -> finish());
    }

    private boolean validateRequired(EditText input, String message) {
        if (Ui.text(input).isEmpty()) {
            input.setError(message);
            input.requestFocus();
            return false;
        }
        return true;
    }

    private void register(CreateUserRequest request, String email) {
        setLoading(true);
        resultText.setText("Đang tạo hồ sơ khách hàng...");
        ApiClient.getApi().register(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    resultText.setText(ApiErrorUtils.httpError(
                            TAG,
                            response,
                            "User-Service hoặc Keycloak đang gặp lỗi. Vui lòng kiểm tra backend."
                    ));
                    return;
                }

                AppSession.saveUserEmail(RegisterActivity.this, email);
                resultText.setText("Đăng ký thành công\n"
                        + Ui.formatBody(response.body())
                        + "\n\nBackend hiện chưa trả User ID trong response đăng ký. Khi đã có User ID do hệ thống cấp, nhập bên dưới để mở tài khoản.");
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                setLoading(false);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void createAccount(EditText userIdInput) {
        String userIdValue = Ui.text(userIdInput);
        if (userIdValue.isEmpty()) {
            userIdInput.setError("Vui lòng nhập User ID");
            userIdInput.requestFocus();
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(userIdValue);
        } catch (NumberFormatException ex) {
            userIdInput.setError("User ID không hợp lệ");
            userIdInput.requestFocus();
            return;
        }

        setAccountLoading(true);
        resultText.setText("Đang mở tài khoản tiết kiệm...");
        AccountRequest request = new AccountRequest("SAVINGS_ACCOUNT", BigDecimal.ZERO, userId);
        ApiClient.getApi().createAccount(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (!response.isSuccessful()) {
                    setAccountLoading(false);
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể mở tài khoản."));
                    return;
                }
                loadCreatedAccount(userId, userIdValue, "Mở tài khoản thành công.\n" + Ui.formatBody(response.body()));
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                setAccountLoading(false);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void loadCreatedAccount(long userId, String userIdValue, String successPrefix) {
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                setAccountLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    String message = response.isSuccessful()
                            ? "Backend chưa trả thông tin tài khoản."
                            : ApiErrorUtils.httpError(TAG, response, "Chưa đọc lại được thông tin tài khoản.");
                    resultText.setText(successPrefix + "\n\n" + message);
                    return;
                }

                AccountResponse account = response.body();
                AppSession.saveUserId(RegisterActivity.this, userIdValue);
                AppSession.saveAccount(RegisterActivity.this, account);
                AppSession.saveRememberedUser(RegisterActivity.this, userIdValue, "User ID " + userIdValue);
                resultText.setText(successPrefix
                        + "\n\nUser ID: " + userIdValue
                        + "\nSố tài khoản: " + account.accountNumber
                        + "\nTrạng thái: " + account.accountStatus
                        + "\nSố dư: " + account.availableBalance
                        + "\n\nTài khoản mới thường ở trạng thái PENDING. Vui lòng nạp tối thiểu 1000 và kích hoạt trước khi chuyển/rút tiền. Sau đó quay lại đăng nhập.");
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                setAccountLoading(false);
                Log.e(TAG, "Cannot load created account", throwable);
                resultText.setText(successPrefix + "\n\nKhông kết nối được server để đọc lại thông tin tài khoản.");
            }
        });
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        openAccountButton.setEnabled(!loading);
        registerButton.setText(loading ? "Đang tạo hồ sơ..." : "Tạo hồ sơ khách hàng");
    }

    private void setAccountLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        openAccountButton.setEnabled(!loading);
        openAccountButton.setText(loading ? "Đang mở tài khoản..." : "Mở tài khoản bằng User ID");
    }
}
