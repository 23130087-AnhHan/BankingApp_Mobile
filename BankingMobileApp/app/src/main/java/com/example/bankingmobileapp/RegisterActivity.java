package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.CreateUserRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends Activity {
    private static final String TAG = "RegisterActivity";

    private Button registerButton;
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
        registerButton = findViewById(R.id.registerButton);
        resultText = findViewById(R.id.resultText);

        registerButton.setOnClickListener(v -> {
            if (!validateRequired(firstNameInput, "Vui lòng nhập tên")
                    || !validateRequired(lastNameInput, "Vui lòng nhập họ và tên đệm")
                    || !validateRequired(phoneInput, "Vui lòng nhập số điện thoại")
                    || !validateRequired(emailInput, "Vui lòng nhập email")
                    || !validateRequired(passwordInput, "Vui lòng nhập mật khẩu")) {
                return;
            }

            String email = Ui.text(emailInput).toLowerCase();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Email không hợp lệ");
                emailInput.requestFocus();
                return;
            }
            if (Ui.text(passwordInput).length() < 8) {
                passwordInput.setError("Mật khẩu phải có ít nhất 8 ký tự");
                passwordInput.requestFocus();
                return;
            }

            CreateUserRequest request = new CreateUserRequest(
                    Ui.text(firstNameInput),
                    Ui.text(lastNameInput),
                    Ui.text(phoneInput),
                    email,
                    Ui.text(passwordInput)
            );
            register(request, email);
        });

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
        showMessage("Đang tạo hồ sơ khách hàng...");
        ApiClient.getApi().register(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showMessage(ApiErrorUtils.httpError(
                            TAG,
                            response,
                            "Không thể tạo hồ sơ lúc này. Vui lòng thử lại."
                    ));
                    return;
                }

                AppSession.clearLoginState(RegisterActivity.this);
                AppSession.clearAccount(RegisterActivity.this);
                AppSession.saveUserEmail(RegisterActivity.this, email);
                Toast.makeText(RegisterActivity.this,
                        "Tạo hồ sơ thành công. Vui lòng đăng nhập để tiếp tục.",
                        Toast.LENGTH_LONG).show();
                Ui.openAndClear(RegisterActivity.this, LoginActivity.class);
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                setLoading(false);
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        registerButton.setText(loading ? "Đang tạo hồ sơ..." : "Tạo hồ sơ");
    }

    private void showMessage(String message) {
        resultText.setVisibility(View.VISIBLE);
        resultText.setText(message);
    }
}
