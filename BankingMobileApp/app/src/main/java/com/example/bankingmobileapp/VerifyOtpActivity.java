package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.ResendEmailOtpRequest;
import com.example.bankingmobileapp.model.VerifyEmailOtpRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends Activity {
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_FLOW = "flow";
    public static final int FLOW_REGISTER = 0;
    public static final int FLOW_RESET_PASSWORD = 1;
    public static final int FLOW_TRANSFER = 2;

    private static final String TAG = "VerifyOtpActivity";
    private static final String STATE_EXPIRES_AT = "otp_expires_at";
    private static final long OTP_DURATION_MILLIS = 5 * 60 * 1000L;

    private String email;
    private int flow;
    private EditText otpInput;
    private Button verifyButton;
    private Button resendButton;
    private TextView countdownText;
    private TextView resultText;
    private CountDownTimer countDownTimer;
    private long expiresAtMillis;
    private boolean otpExpired;
    private boolean requestInProgress;
    private boolean resendRequestInProgress;
    private boolean activityStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        flow = getIntent().getIntExtra(EXTRA_FLOW, FLOW_REGISTER);
        if (email == null || email.trim().isEmpty()) {
            email = AppSession.getUserEmail(this);
        }
        email = email.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy email cần xác thực.", Toast.LENGTH_LONG).show();
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }

        TextView destinationText = findViewById(R.id.otpDestinationText);
        otpInput = findViewById(R.id.otpInput);
        verifyButton = findViewById(R.id.verifyOtpButton);
        resendButton = findViewById(R.id.resendOtpButton);
        countdownText = findViewById(R.id.otpCountdownText);
        resultText = findViewById(R.id.resultText);

        expiresAtMillis = savedInstanceState == null
                ? System.currentTimeMillis() + OTP_DURATION_MILLIS
                : savedInstanceState.getLong(
                        STATE_EXPIRES_AT, System.currentTimeMillis() + OTP_DURATION_MILLIS);

        destinationText.setText("Mã OTP đã được gửi đến email: " + email);
        verifyButton.setOnClickListener(v -> verifyOtp());
        resendButton.setOnClickListener(v -> resendOtp());
        Button backButton = findViewById(R.id.backToLoginButton);
        if (flow == FLOW_TRANSFER) {
            backButton.setText("Hủy giao dịch");
            backButton.setOnClickListener(v -> finish());
        } else {
            backButton.setText("Quay về đăng nhập");
            backButton.setOnClickListener(v -> Ui.openAndClear(this, WelcomeActivity.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (countdownText == null) {
            return;
        }
        activityStarted = true;
        startCountdown(expiresAtMillis - System.currentTimeMillis());
    }

    @Override
    protected void onStop() {
        activityStarted = false;
        cancelCountdown();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        cancelCountdown();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(STATE_EXPIRES_AT, expiresAtMillis);
        super.onSaveInstanceState(outState);
    }

    private void verifyOtp() {
        if (requestInProgress) {
            return;
        }
        if (otpExpired) {
            showMessage("OTP đã hết hạn. Vui lòng gửi lại mã.");
            return;
        }
        String otp = Ui.text(otpInput);
        if (otp.isEmpty() || otp.length() != 6) {
            otpInput.setError("OTP phải gồm 6 chữ số");
            otpInput.requestFocus();
            return;
        }

        if (flow == FLOW_TRANSFER) {
            // Returning the OTP back to the activity that started this one
            android.content.Intent data = new android.content.Intent();
            data.putExtra("otp", otp);
            setResult(RESULT_OK, data);
            finish();
            return;
        }

        if (flow == FLOW_RESET_PASSWORD) {
            // Navigate to Password Update screen
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("otp", otp);
            startActivity(intent);
            finish();
            return;
        }

        setLoading(true, false, "Đang xác thực...");
        Log.d(TAG, "POST " + ApiClient.getAuthBaseUrl()
                + "api/users/auth/verify-email-otp body={email=" + email + ", otp=<redacted>}");
        ApiClient.getAuthApi().verifyEmailOtp(new VerifyEmailOtpRequest(email, otp))
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        Log.d(TAG, "Verify OTP HTTP status=" + response.code()
                                + " message=" + response.message());
                        setLoading(false, false, null);
                        if (!response.isSuccessful() || response.body() == null) {
                            showMessage(ApiErrorUtils.httpError(
                                    TAG, response, "Không thể xác thực OTP lúc này."));
                            return;
                        }
                        Toast.makeText(VerifyOtpActivity.this,
                                "Xác thực email thành công", Toast.LENGTH_LONG).show();
                        Ui.openAndClear(VerifyOtpActivity.this, WelcomeActivity.class);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                        setLoading(false, false, null);
                        showMessage(ApiErrorUtils.networkError(TAG, throwable));
                    }
                });
    }

    private void resendOtp() {
        if (requestInProgress) {
            return;
        }
        if (!otpExpired) {
            showMessage("Bạn có thể gửi lại mã khi bộ đếm kết thúc.");
            return;
        }

        setLoading(true, true, "Đang gửi lại mã...");
        Call<ApiResponse> call;
        if (flow == FLOW_TRANSFER) {
            Log.d(TAG, "POST " + ApiClient.getAuthBaseUrl()
                    + "api/users/auth/payment-otp/send body={email=" + email + "}");
            call = ApiClient.getAuthApi().sendPaymentOtp(new com.example.bankingmobileapp.model.SendPaymentOtpRequest(email));
        } else {
            Log.d(TAG, "POST " + ApiClient.getAuthBaseUrl()
                    + "api/users/auth/resend-email-otp body={email=" + email + "}");
            call = ApiClient.getAuthApi().resendEmailOtp(new ResendEmailOtpRequest(email));
        }

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                Log.d(TAG, "Resend OTP HTTP status=" + response.code()
                        + " message=" + response.message());
                setLoading(false, false, null);
                if (!response.isSuccessful() || response.body() == null) {
                    showMessage(ApiErrorUtils.httpError(
                            TAG, response, "Không thể gửi lại OTP lúc này."));
                    return;
                }
                String message = response.body().responseMessage;
                if (message == null || message.trim().isEmpty()) {
                    message = "Đã gửi lại OTP. Vui lòng kiểm tra email";
                }
                Toast.makeText(VerifyOtpActivity.this, message, Toast.LENGTH_LONG).show();
                showMessage(message);
                otpInput.setText("");
                otpInput.requestFocus();
                expiresAtMillis = System.currentTimeMillis() + OTP_DURATION_MILLIS;
                if (activityStarted) {
                    startCountdown(OTP_DURATION_MILLIS);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                setLoading(false, false, null);
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void setLoading(boolean loading, boolean resending, String message) {
        requestInProgress = loading;
        resendRequestInProgress = loading && resending;
        updateButtons();
        if (message != null) {
            showMessage(message);
        }
    }

    private void startCountdown(long durationMillis) {
        cancelCountdown();
        if (durationMillis <= 0) {
            markOtpExpired();
            return;
        }

        otpExpired = false;
        updateCountdown(durationMillis);
        updateButtons();
        countDownTimer = new CountDownTimer(durationMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateCountdown(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                countDownTimer = null;
                markOtpExpired();
            }
        }.start();
    }

    private void updateCountdown(long millisUntilFinished) {
        long totalSeconds = (millisUntilFinished + 999L) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        countdownText.setText(String.format(
                Locale.getDefault(), "Mã OTP còn hiệu lực trong %02d:%02d", minutes, seconds));
        if (!resendRequestInProgress) {
            resendButton.setText(String.format(
                    Locale.getDefault(), "Gửi lại mã (%02d:%02d)", minutes, seconds));
        }
    }

    private void markOtpExpired() {
        otpExpired = true;
        countdownText.setText("OTP hết hạn. Vui lòng gửi lại mã.");
        updateButtons();
    }

    private void updateButtons() {
        boolean verifyEnabled = !requestInProgress && !otpExpired;
        boolean resendEnabled = !requestInProgress && otpExpired;
        verifyButton.setEnabled(verifyEnabled);
        resendButton.setEnabled(resendEnabled);
        verifyButton.setAlpha(verifyEnabled ? 1f : 0.55f);
        resendButton.setAlpha(resendEnabled ? 1f : 0.55f);
        verifyButton.setText(requestInProgress && !resendRequestInProgress
                ? "Đang xác thực..." : "Xác thực");
        if (resendRequestInProgress) {
            resendButton.setText("Đang gửi lại mã...");
        } else if (otpExpired) {
            resendButton.setText("Gửi lại mã");
        }
    }

    private void cancelCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void showMessage(String message) {
        resultText.setVisibility(View.VISIBLE);
        resultText.setText(message);
    }
}
