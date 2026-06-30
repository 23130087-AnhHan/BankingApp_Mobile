package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.ResetPasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends Activity {
    private static final String TAG = "ResetPasswordActivity";

    private String email;
    private String otp;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button submitButton;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Reusing register layout for password inputs

        email = getIntent().getStringExtra("email");
        otp = getIntent().getStringExtra("otp");

        // UI setup - hide unnecessary fields from register layout
        findViewById(R.id.firstNameInput).setVisibility(android.view.View.GONE);
        findViewById(R.id.lastNameInput).setVisibility(android.view.View.GONE);
        findViewById(R.id.phoneInput).setVisibility(android.view.View.GONE);
        findViewById(R.id.emailInput).setVisibility(android.view.View.GONE);
        findViewById(R.id.loginButton).setVisibility(android.view.View.GONE);
        ((TextView)findViewById(R.id.registerButton)).setText("Cập nhật mật khẩu");

        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        submitButton = findViewById(R.id.registerButton);
        resultText = findViewById(R.id.resultText);

        Ui.configurePasswordVisibility(passwordInput);
        Ui.configurePasswordVisibility(confirmPasswordInput);

        submitButton.setOnClickListener(v -> submit());
    }

    private void submit() {
        String password = passwordInput.getText().toString();
        String confirm = confirmPasswordInput.getText().toString();

        if (password.isEmpty() || password.length() < 8) {
            passwordInput.setError("Mật khẩu phải có ít nhất 8 ký tự");
            return;
        }
        if (!password.equals(confirm)) {
            confirmPasswordInput.setError("Mật khẩu không khớp");
            return;
        }

        resultText.setText("Đang cập nhật mật khẩu...");
        submitButton.setEnabled(false);

        ResetPasswordRequest request = new ResetPasswordRequest(email, otp, password);
        ApiClient.getApi().resetPassword(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                submitButton.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, "Cập nhật mật khẩu thành công", Toast.LENGTH_LONG).show();
                    Ui.openAndClear(ResetPasswordActivity.this, LoginActivity.class);
                } else {
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Lỗi cập nhật mật khẩu"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                submitButton.setEnabled(true);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }
}
