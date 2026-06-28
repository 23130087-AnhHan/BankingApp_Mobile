package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.CreateUserRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends Activity {
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText firstNameInput = findViewById(R.id.firstNameInput);
        EditText lastNameInput = findViewById(R.id.lastNameInput);
        EditText phoneInput = findViewById(R.id.phoneInput);
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        TextView resultText = findViewById(R.id.resultText);
        Button registerButton = findViewById(R.id.registerButton);
        Button openAccountButton = findViewById(R.id.openAccountButton);

        openAccountButton.setOnClickListener(v -> Ui.open(this, AccountActivity.class));
        registerButton.setOnClickListener(v -> {
            if (!validateRequired(firstNameInput, "Vui lòng nhập tên")
                    || !validateRequired(lastNameInput, "Vui lòng nhập họ")
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

            resultText.setText("Đang tạo hồ sơ khách hàng...");
            registerButton.setEnabled(false);
            ApiClient.getApi().register(request).enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    registerButton.setEnabled(true);
                    if (!response.isSuccessful()) {
                        resultText.setText(ApiErrorUtils.httpError(
                                TAG,
                                response,
                                "User-Service hoặc Keycloak đang gặp lỗi. Vui lòng kiểm tra backend."
                        ));
                        return;
                    }

                    AppSession.clearSession(RegisterActivity.this);
                    AppSession.saveUserEmail(RegisterActivity.this, email);

                    // Response hiện chỉ có responseCode/responseMessage/message, không có userId để lưu an toàn.
                    resultText.setText("Đăng ký thành công\n"
                            + Ui.formatBody(response.body())
                            + "\n\nAPI chưa trả mã khách hàng. Hãy tiếp tục sang Tài khoản và nhập User ID một lần khi đã có.");
                    openAccountButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                    registerButton.setEnabled(true);
                    resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
                }
            });
        });
    }

    private boolean validateRequired(EditText input, String message) {
        if (Ui.text(input).isEmpty()) {
            input.setError(message);
            input.requestFocus();
            return false;
        }
        return true;
    }
}
