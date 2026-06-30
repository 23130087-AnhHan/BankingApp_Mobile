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
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.AuthResponse;
import com.example.bankingmobileapp.model.ForgotPasswordRequest;
import com.example.bankingmobileapp.model.LoginRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends Activity {
    private static final String TAG = "LoginActivity";
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSession.clearLoginState(this);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.userIdInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        resultText = findViewById(R.id.resultText);

        String rememberedEmail = AppSession.getUserEmail(this);
        if (!rememberedEmail.isEmpty()) {
            emailInput.setText(rememberedEmail);
        }

        loginButton.setOnClickListener(v -> login());
        findViewById(R.id.registerButton).setOnClickListener(v -> Ui.open(this, RegisterActivity.class));
        findViewById(R.id.forgotPasswordButton).setOnClickListener(v -> requestPasswordReset());
    }

    private void requestPasswordReset() {
        String email = Ui.text(emailInput).toLowerCase();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Nhập email hợp lệ để đặt lại mật khẩu");
            emailInput.requestFocus();
            return;
        }
        ApiClient.getApi().forgotPassword(new ForgotPasswordRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Toast.makeText(LoginActivity.this,
                        "Nếu email đã đăng ký, hướng dẫn đặt lại mật khẩu sẽ được gửi.",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void login() {
        String email = Ui.text(emailInput).toLowerCase();
        String password = Ui.text(passwordInput);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Email không hợp lệ");
            emailInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Vui lòng nhập mật khẩu");
            passwordInput.requestFocus();
            return;
        }

        setLoading(true);
        showMessage("Đang xác thực...");
        ApiClient.getApi().login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setLoading(false);
                    AppSession.clearLoginState(LoginActivity.this);
                    showMessage(response.code() == 401
                            ? "Email hoặc mật khẩu không đúng."
                            : ApiErrorUtils.httpError(TAG, response, "Không thể đăng nhập lúc này."));
                    return;
                }
                AuthResponse auth = response.body();
                if (auth.userId == null || auth.accessToken == null || auth.accessToken.isEmpty()) {
                    setLoading(false);
                    showMessage("Phiên đăng nhập từ server không hợp lệ.");
                    return;
                }
                AppSession.saveAuth(LoginActivity.this, auth);
                AppSession.saveLoginState(LoginActivity.this, true);
                loadAccountAndContinue(auth.userId);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                setLoading(false);
                AppSession.clearLoginState(LoginActivity.this);
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void loadAccountAndContinue(long userId) {
        showMessage("Đăng nhập thành công. Đang đồng bộ tài khoản...");
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AppSession.saveAccount(LoginActivity.this, response.body());
                } else if (response.code() == 404 || response.code() == 400) {
                    AppSession.clearAccount(LoginActivity.this);
                }
                Ui.openAndClear(LoginActivity.this, MainActivity.class);
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                setLoading(false);
                Ui.openAndClear(LoginActivity.this, MainActivity.class);
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void showMessage(String message) {
        resultText.setVisibility(View.VISIBLE);
        resultText.setText(message);
    }
}
