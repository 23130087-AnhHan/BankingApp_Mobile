package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
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
            if (!validate(firstNameInput, lastNameInput, phoneInput, emailInput, passwordInput)) {
                return;
            }
            CreateUserRequest request = new CreateUserRequest(
                    Ui.text(firstNameInput),
                    Ui.text(lastNameInput),
                    Ui.text(phoneInput),
                    Ui.text(emailInput),
                    Ui.text(passwordInput)
            );
            register(request);
        });
        openAccountButton.setOnClickListener(v -> createAccount(userIdInput));
        findViewById(R.id.loginButton).setOnClickListener(v -> finish());
    }

    private boolean validate(EditText firstNameInput, EditText lastNameInput, EditText phoneInput,
                             EditText emailInput, EditText passwordInput) {
        if (Ui.text(firstNameInput).isEmpty()) {
            firstNameInput.setError("Vui lòng nhập tên");
            return false;
        }
        if (Ui.text(lastNameInput).isEmpty()) {
            lastNameInput.setError("Vui lòng nhập họ và tên đệm");
            return false;
        }
        if (Ui.text(phoneInput).isEmpty()) {
            phoneInput.setError("Vui lòng nhập số điện thoại");
            return false;
        }
        if (Ui.text(emailInput).isEmpty()) {
            emailInput.setError("Vui lòng nhập email");
            return false;
        }
        if (Ui.text(passwordInput).isEmpty()) {
            passwordInput.setError("Vui lòng nhập mật khẩu");
            return false;
        }
        return true;
    }

    private void register(CreateUserRequest request) {
        setLoading(true);
        resultText.setText("Đang tạo hồ sơ khách hàng...");
        ApiClient.getApi().register(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Register failed. HTTP " + response.code());
                    resultText.setText("Đăng ký thất bại.\n" + Ui.messageForHttpCode(response.code()));
                    return;
                }

                resultText.setText("Đăng ký thành công.\n"
                        + Ui.formatBody(response.body())
                        + "\n\nBackend hiện chưa trả User ID trong response đăng ký. Vui lòng duyệt hồ sơ nếu cần, sau đó dùng User ID để mở tài khoản và đăng nhập.");
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                setLoading(false);
                Log.e(TAG, "Register network failure", throwable);
                resultText.setText("Đăng ký thất bại.\nKhông kết nối được server.");
            }
        });
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        openAccountButton.setEnabled(!loading);
        registerButton.setText(loading ? "Đang tạo hồ sơ..." : "Tạo hồ sơ khách hàng");
    }

    private void createAccount(EditText userIdInput) {
        String userIdValue = Ui.text(userIdInput);
        if (userIdValue.isEmpty()) {
            userIdInput.setError("Vui lòng nhập User ID");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(userIdValue);
        } catch (NumberFormatException ex) {
            userIdInput.setError("User ID không hợp lệ");
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
                    Log.e(TAG, "Create account failed. HTTP " + response.code());
                    resultText.setText("Mở tài khoản thất bại.\n" + Ui.messageForHttpCode(response.code()));
                    return;
                }
                loadCreatedAccount(userId, userIdValue);
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                setAccountLoading(false);
                Log.e(TAG, "Create account network failure", throwable);
                resultText.setText("Mở tài khoản thất bại.\nKhông kết nối được server.");
            }
        });
    }

    private void loadCreatedAccount(long userId, String userIdValue) {
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                setAccountLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Load created account failed. HTTP " + response.code());
                    resultText.setText("Hồ sơ/tài khoản đã gửi xử lý, nhưng chưa đọc lại được thông tin tài khoản.\n"
                            + Ui.messageForHttpCode(response.code()));
                    return;
                }

                AccountResponse account = response.body();
                AppSession.saveUserId(RegisterActivity.this, userIdValue);
                AppSession.saveAccount(RegisterActivity.this, account);
                AppSession.saveRememberedUser(RegisterActivity.this, userIdValue, "User ID " + userIdValue);
                resultText.setText("Mở tài khoản thành công."
                        + "\nUser ID: " + userIdValue
                        + "\nSố tài khoản: " + account.accountNumber
                        + "\nTrạng thái: " + account.accountStatus
                        + "\nSố dư: " + account.availableBalance
                        + "\n\nTài khoản mới thường ở trạng thái PENDING. Vui lòng nạp tối thiểu 1000 và kích hoạt trước khi chuyển/rút tiền. Sau đó quay lại đăng nhập.");
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                setAccountLoading(false);
                Log.e(TAG, "Load created account network failure", throwable);
                resultText.setText("Mở tài khoản đã gửi xử lý, nhưng không kết nối được server để đọc lại thông tin.");
            }
        });
    }

    private void setAccountLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        openAccountButton.setEnabled(!loading);
        openAccountButton.setText(loading ? "Đang mở tài khoản..." : "Mở tài khoản bằng User ID");
    }
}
