package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.model.AccountResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends Activity {
    private static final String TAG = "LoginActivity";

    private EditText userIdInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSession.clearLoginState(this);

        setContentView(R.layout.activity_login);

        userIdInput = findViewById(R.id.userIdInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        resultText = findViewById(R.id.resultText);

        loginButton.setOnClickListener(v -> loginWithDemoFallback());
        findViewById(R.id.registerButton).setOnClickListener(v -> Ui.open(this, RegisterActivity.class));
        findViewById(R.id.forgotPasswordButton).setOnClickListener(v ->
                Toast.makeText(this, "Tính năng quên mật khẩu chưa hỗ trợ trong phiên bản demo.", Toast.LENGTH_SHORT).show());
    }

    private void loginWithDemoFallback() {
        String userIdValue = Ui.text(userIdInput);
        if (userIdValue.isEmpty()) {
            userIdInput.setError("Vui lòng nhập User ID");
            return;
        }
        String password = Ui.text(passwordInput);
        if (password.isEmpty()) {
            passwordInput.setError("Vui lòng nhập mật khẩu");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(userIdValue);
        } catch (NumberFormatException ex) {
            userIdInput.setError("User ID không hợp lệ");
            return;
        }

        setLoading(true);
        resultText.setText("Đang xác thực phiên bản demo...");
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Login failed. HTTP " + response.code());
                    AppSession.saveLoginState(LoginActivity.this, false);
                    if (response.code() == 404) {
                        resultText.setText("Không tìm thấy tài khoản cho User ID này. Vui lòng mở tài khoản trước.");
                    } else {
                        resultText.setText(Ui.messageForHttpCode(response.code()));
                    }
                    return;
                }

                AccountResponse account = response.body();
                AppSession.saveUserId(LoginActivity.this, userIdValue);
                AppSession.saveAccount(LoginActivity.this, account);
                AppSession.saveRememberedUser(LoginActivity.this, userIdValue, "User ID " + userIdValue);
                AppSession.saveLoginState(LoginActivity.this, true);
                Ui.openAndClear(LoginActivity.this, MainActivity.class);
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                setLoading(false);
                Log.e(TAG, "Login network failure", throwable);
                AppSession.saveLoginState(LoginActivity.this, false);
                resultText.setText("Không kết nối được server. Vui lòng kiểm tra backend và thử lại.");
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }
}
