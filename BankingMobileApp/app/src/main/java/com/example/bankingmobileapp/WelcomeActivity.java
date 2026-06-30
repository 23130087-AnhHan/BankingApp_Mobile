package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.AuthResponse;
import com.example.bankingmobileapp.model.ForgotPasswordRequest;
import com.example.bankingmobileapp.model.LoginRequest;
import com.example.bankingmobileapp.model.ResendEmailOtpRequest;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends Activity {
    private static final String TAG = "WelcomeActivity";
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button verifyEmailButton;
    private TextView resultText;
    private boolean loginLoading;
    private boolean verificationLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSession.lockSession(this);

        if (AppSession.hasRememberedUser(this)) {
            Ui.openAndClear(this, QuickLoginActivity.class);
            return;
        }

        AppSession.clearLoginState(this);
        setContentView(R.layout.activity_welcome);

        emailInput = findViewById(R.id.edtUsername);
        passwordInput = findViewById(R.id.edtPassword);
        loginButton = findViewById(R.id.loginButton);
        verifyEmailButton = findViewById(R.id.verifyEmailButton);
        resultText = findViewById(R.id.resultText);
        Ui.configurePasswordVisibility(passwordInput);

        String rememberedEmail = AppSession.getUserEmail(this);
        if (!rememberedEmail.isEmpty()) {
            emailInput.setText(rememberedEmail);
        }

        loginButton.setOnClickListener(v -> login());
        if (verifyEmailButton != null) {
            verifyEmailButton.setOnClickListener(v -> resendVerificationOtp());
        }
        findViewById(R.id.txtRegister).setOnClickListener(v -> Ui.open(this, RegisterActivity.class));
        findViewById(R.id.txtForgot).setOnClickListener(v -> requestPasswordReset());
        findViewById(R.id.btnLanguage).setOnClickListener(v -> showDemoToast());
        findViewById(R.id.btnSupport).setOnClickListener(v -> showDemoToast());
    }

    private void showDemoToast() {
        Toast.makeText(this, "Tính năng đang được phát triển", Toast.LENGTH_SHORT).show();
    }

    private void requestPasswordReset() {
        String email = Ui.text(emailInput).toLowerCase(Locale.ROOT);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Nhập email hợp lệ để đặt lại mật khẩu");
            emailInput.requestFocus();
            return;
        }
        ApiClient.getAuthApi().forgotPassword(new ForgotPasswordRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Toast.makeText(WelcomeActivity.this,
                        "Mã xác thực OTP đã được gửi đến email của bạn.",
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(WelcomeActivity.this, VerifyOtpActivity.class);
                intent.putExtra(VerifyOtpActivity.EXTRA_EMAIL, email);
                intent.putExtra(VerifyOtpActivity.EXTRA_FLOW, VerifyOtpActivity.FLOW_RESET_PASSWORD);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void login() {
        String email = Ui.text(emailInput).toLowerCase(Locale.ROOT);
        String password = passwordInput.getText().toString();
        showVerificationAction(false);
        if (email.isEmpty()) {
            emailInput.setError("Email không được để trống");
            emailInput.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Email không đúng định dạng");
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
        Log.d(TAG, "POST " + ApiClient.getAuthBaseUrl()
                + "api/users/auth/login body={email=" + email + ", password=<redacted>}");
        ApiClient.getAuthApi().login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                Log.d(TAG, "Login HTTP status=" + response.code()
                        + " message=" + response.message());
                if (!response.isSuccessful() || response.body() == null) {
                    setLoading(false);
                    AppSession.clearLoginState(WelcomeActivity.this);
                    String errorMessage = ApiErrorUtils.httpError(
                            TAG, response, "Không thể đăng nhập lúc này.");
                    if (response.code() == 401) {
                        errorMessage = isVerificationRequired(errorMessage)
                                ? "Vui lòng xác thực email trước khi đăng nhập"
                                : "Tài khoản hoặc mật khẩu không chính xác";
                    }
                    showMessage(errorMessage);
                    showVerificationAction(isVerificationRequired(errorMessage));
                    return;
                }
                AuthResponse auth = response.body();
                if (auth.userId == null || auth.accessToken == null || auth.accessToken.isEmpty()) {
                    setLoading(false);
                    showMessage("Phiên đăng nhập từ server không hợp lệ.");
                    return;
                }
                AppSession.saveAuth(WelcomeActivity.this, auth);
                AppSession.saveLoginState(WelcomeActivity.this, true);
                loadAccountAndContinue(auth.userId);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                setLoading(false);
                AppSession.clearLoginState(WelcomeActivity.this);
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void resendVerificationOtp() {
        String email = Ui.text(emailInput).toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            emailInput.setError("Email không được để trống");
            emailInput.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Email không đúng định dạng");
            emailInput.requestFocus();
            return;
        }

        setVerificationLoading(true);
        showMessage("Đang gửi lại mã xác thực...");
        Log.d(TAG, "POST " + ApiClient.getAuthBaseUrl()
                + "api/users/auth/resend-email-otp body={email=" + email + "}");
        ApiClient.getAuthApi().resendEmailOtp(new ResendEmailOtpRequest(email))
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        Log.d(TAG, "Resend verification OTP HTTP status=" + response.code()
                                + " message=" + response.message());
                        setVerificationLoading(false);
                        if (!response.isSuccessful() || response.body() == null) {
                            showMessage(ApiErrorUtils.httpError(TAG, response,
                                    "Không thể gửi lại OTP lúc này."));
                            return;
                        }

                        String message = firstNonEmpty(response.body().responseMessage,
                                response.body().message);
                        if (isAlreadyVerified(message)) {
                            showVerificationAction(false);
                            showMessage("Email đã được xác thực. Vui lòng đăng nhập lại.");
                            return;
                        }

                        AppSession.saveUserEmail(WelcomeActivity.this, email);
                        Intent intent = new Intent(WelcomeActivity.this, VerifyOtpActivity.class);
                        intent.putExtra(VerifyOtpActivity.EXTRA_EMAIL, email);
                        intent.putExtra(VerifyOtpActivity.EXTRA_FLOW, VerifyOtpActivity.FLOW_REGISTER);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                        setVerificationLoading(false);
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
                    AppSession.saveAccount(WelcomeActivity.this, response.body());
                } else if (response.code() == 404 || response.code() == 400) {
                    AppSession.clearAccount(WelcomeActivity.this);
                }
                Ui.openAndClear(WelcomeActivity.this, MainActivity.class);
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                setLoading(false);
                Ui.openAndClear(WelcomeActivity.this, MainActivity.class);
            }
        });
    }

    private void setLoading(boolean loading) {
        loginLoading = loading;
        updateLoadingState();
        loginButton.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }

    private void setVerificationLoading(boolean loading) {
        verificationLoading = loading;
        updateLoadingState();
        if (verifyEmailButton != null) {
            verifyEmailButton.setText(loading ? "Đang gửi lại mã..." : "Xác thực email");
        }
    }

    private void updateLoadingState() {
        boolean idle = !loginLoading && !verificationLoading;
        loginButton.setEnabled(idle);
        if (verifyEmailButton != null) {
            verifyEmailButton.setEnabled(idle);
        }
    }

    private void showVerificationAction(boolean visible) {
        if (verifyEmailButton != null) {
            verifyEmailButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isVerificationRequired(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("xác thực email")
                || normalized.contains("verify email")
                || normalized.contains("email not verified");
    }

    private boolean isAlreadyVerified(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("đã được xác thực")
                || normalized.contains("already verified");
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }

    private void showMessage(String message) {
        if (resultText != null) {
            resultText.setVisibility(View.VISIBLE);
            resultText.setText(message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
}
