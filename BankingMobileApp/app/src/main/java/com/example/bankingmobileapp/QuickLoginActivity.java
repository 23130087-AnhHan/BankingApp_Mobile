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

public class QuickLoginActivity extends Activity {
    private static final String TAG = "QuickLoginActivity";

    private EditText passwordInput;
    private Button loginButton;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSession.clearLoginState(this);
        setContentView(R.layout.activity_quick_login);

        TextView greetingText = findViewById(R.id.greetingText);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        resultText = findViewById(R.id.resultText);

        String displayName = AppSession.getRememberedDisplayName(this);
        greetingText.setText(displayName.isEmpty() ? "Xin chào" : "Xin chào, " + displayName);

        loginButton.setOnClickListener(v -> loginRememberedUser());
        findViewById(R.id.otherAccountButton).setOnClickListener(v -> {
            AppSession.clearLoginState(this);
            Ui.openAndClear(this, LoginActivity.class);
        });
        findViewById(R.id.forgotPasswordButton).setOnClickListener(v ->
                Toast.makeText(this, "Tính năng quên mật khẩu chưa hỗ trợ trong phiên bản demo.", Toast.LENGTH_SHORT).show());
    }

    private void loginRememberedUser() {
        String password = Ui.text(passwordInput);
        if (password.isEmpty()) {
            passwordInput.setError("Vui lòng nhập mật khẩu");
            return;
        }

        String userIdValue = AppSession.getRememberedUserId(this);
        if (userIdValue.isEmpty()) {
            resultText.setText("Không tìm thấy tài khoản đã nhớ. Vui lòng đăng nhập bằng tài khoản khác.");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(userIdValue);
        } catch (NumberFormatException ex) {
            resultText.setText("User ID đã lưu không hợp lệ. Vui lòng chọn tài khoản khác.");
            return;
        }

        setLoading(true);
        resultText.setText("Đang xác thực phiên demo...");
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "Quick login failed. HTTP " + response.code());
                    AppSession.clearLoginState(QuickLoginActivity.this);
                    resultText.setText(response.code() == 404
                            ? "Không tìm thấy tài khoản đã lưu."
                            : Ui.messageForHttpCode(response.code()));
                    return;
                }

                AccountResponse account = response.body();
                AppSession.saveUserId(QuickLoginActivity.this, userIdValue);
                AppSession.saveAccount(QuickLoginActivity.this, account);
                AppSession.saveRememberedUser(QuickLoginActivity.this, userIdValue, "User ID " + userIdValue);
                AppSession.saveLoginState(QuickLoginActivity.this, true);
                Ui.openAndClear(QuickLoginActivity.this, MainActivity.class);
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                setLoading(false);
                Log.e(TAG, "Quick login network failure", throwable);
                AppSession.clearLoginState(QuickLoginActivity.this);
                resultText.setText("Không kết nối được server.");
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Đang đăng nhập..." : "Đăng nhập");
    }
}
