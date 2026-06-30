package com.example.bankingmobileapp;

import android.os.Bundle;
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

import java.util.concurrent.Executor;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuickLoginActivity extends FragmentActivity {
    private static final String TAG = "QuickLoginActivity";
    private EditText passwordInput;
    private Button loginButton;
    private Button biometricLoginButton;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSession.lockSession(this);
        setContentView(R.layout.activity_quick_login);

        TextView greetingText = findViewById(R.id.greetingText);
        passwordInput = findViewById(R.id.passwordInput);
        Ui.configurePasswordVisibility(passwordInput);
        loginButton = findViewById(R.id.loginButton);
        biometricLoginButton = findViewById(R.id.biometricLoginButton);
        resultText = findViewById(R.id.resultText);
        String displayName = AppSession.getRememberedDisplayName(this);
        greetingText.setText(displayName.isEmpty() ? "Xin chào" : "Xin chào, " + displayName);

        loginButton.setOnClickListener(v -> loginRememberedUser());
        biometricLoginButton.setOnClickListener(v -> authenticateWithBiometric());
        findViewById(R.id.otherAccountButton).setOnClickListener(v -> Ui.openAndClear(this, LoginActivity.class));
        findViewById(R.id.forgotPasswordButton).setOnClickListener(v -> requestPasswordReset());
        renderBiometricState();
    }

    private void renderBiometricState() {
        boolean hasStoredSession = !AppSession.getAuthToken(this).isEmpty()
                && !AppSession.getRefreshToken(this).isEmpty();
        int status = BiometricManager.from(this)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        biometricLoginButton.setVisibility(
                hasStoredSession && status == BiometricManager.BIOMETRIC_SUCCESS
                        ? View.VISIBLE
                        : View.GONE);

        if (!hasStoredSession) {
            return;
        }
        if (status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            showMessage("Thiết bị chưa thiết lập vân tay. Vui lòng đăng nhập bằng mật khẩu.");
        } else if (status == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            showMessage("Thiết bị không hỗ trợ đăng nhập bằng vân tay.");
        }
    }

    private void authenticateWithBiometric() {
        if (AppSession.getAuthToken(this).isEmpty() || AppSession.getRefreshToken(this).isEmpty()) {
            showMessage("Phiên đã hết hạn. Vui lòng đăng nhập bằng mật khẩu.");
            biometricLoginButton.setVisibility(View.GONE);
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        AppSession.saveLoginState(QuickLoginActivity.this, true);
                        Ui.openAndClear(QuickLoginActivity.this, MainActivity.class);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            showMessage(errString == null
                                    ? "Không thể xác thực vân tay."
                                    : errString.toString());
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        showMessage("Vân tay không khớp. Vui lòng thử lại.");
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Đăng nhập bằng vân tay")
                .setSubtitle("Xác thực để vào NLU Banking")
                .setNegativeButtonText("Dùng mật khẩu")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void requestPasswordReset() {
        String email = AppSession.getUserEmail(this);
        if (email.isEmpty()) {
            Ui.openAndClear(this, LoginActivity.class);
            return;
        }
        ApiClient.getAuthApi().forgotPassword(new ForgotPasswordRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Toast.makeText(QuickLoginActivity.this,
                        "Nếu email đã đăng ký, hướng dẫn đặt lại mật khẩu sẽ được gửi.",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void loginRememberedUser() {
        String email = AppSession.getUserEmail(this);
        String password = Ui.text(passwordInput);
        if (email.isEmpty()) {
            Ui.openAndClear(this, LoginActivity.class);
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Vui lòng nhập mật khẩu");
            return;
        }
        setLoading(true);
        showMessage("Đang xác thực...");
        ApiClient.getAuthApi().login(new LoginRequest(email, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().userId == null) {
                    setLoading(false);
                    showMessage(response.code() == 401
                            ? "Mật khẩu không đúng."
                            : ApiErrorUtils.httpError(TAG, response, "Không thể đăng nhập."));
                    return;
                }
                AppSession.saveAuth(QuickLoginActivity.this, response.body());
                AppSession.saveLoginState(QuickLoginActivity.this, true);
                loadAccount(response.body().userId);
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable throwable) {
                setLoading(false);
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void loadAccount(long userId) {
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AppSession.saveAccount(QuickLoginActivity.this, response.body());
                } else if (response.code() == 404 || response.code() == 400) {
                    AppSession.clearAccount(QuickLoginActivity.this);
                }
                Ui.openAndClear(QuickLoginActivity.this, MainActivity.class);
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                setLoading(false);
                Ui.openAndClear(QuickLoginActivity.this, MainActivity.class);
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
