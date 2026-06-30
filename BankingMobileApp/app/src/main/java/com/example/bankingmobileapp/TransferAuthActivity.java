package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.FundTransferRequest;
import com.example.bankingmobileapp.model.FundTransferResponse;
import com.example.bankingmobileapp.model.SendPaymentOtpRequest;
import com.example.bankingmobileapp.model.VerifyPaymentOtpRequest;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferAuthActivity extends Activity {
    private static final String TAG = "TransferAuthActivity";
    private EditText codeInput;
    private TextView resultText;
    private Button sendOtpButton;
    private Button submitButton;
    private boolean otpSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }
        setContentView(R.layout.activity_transfer_auth);

        boolean otp = isOtp();
        codeInput = findViewById(R.id.codeInput);
        resultText = findViewById(R.id.resultText);
        sendOtpButton = findViewById(R.id.sendOtpButton);
        submitButton = findViewById(R.id.submitButton);
        codeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        codeInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});

        ((TextView) findViewById(R.id.authTitleText)).setText(otp ? "Xác thực bằng OTP email" : "Xác thực bằng mã PIN");
        ((TextView) findViewById(R.id.authHintText)).setText(otp
                ? "Quý khách vui lòng gửi OTP và nhập mã được gửi về email."
                : "Quý khách vui lòng nhập mã PIN thanh toán để hoàn tất giao dịch.");
        sendOtpButton.setVisibility(otp ? View.VISIBLE : View.GONE);
        findViewById(R.id.managePinButton).setVisibility(!otp && !AppSession.hasPaymentPin(this) ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!otp || otpSent);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.managePinButton).setOnClickListener(v -> Ui.open(this, PinActivity.class));
        sendOtpButton.setOnClickListener(v -> sendOtp());
        submitButton.setOnClickListener(v -> verifyAndSubmit());
    }

    private void sendOtp() {
        String email = AppSession.getUserEmail(this);
        if (email.isEmpty()) {
            resultText.setText("Phiên đăng nhập chưa có email để gửi OTP.");
            return;
        }
        sendOtpButton.setEnabled(false);
        resultText.setText("Đang gửi OTP thanh toán tới email...");
        ApiClient.getApi().sendPaymentOtp(new SendPaymentOtpRequest(email)).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                sendOtpButton.setEnabled(true);
                if (!response.isSuccessful()) {
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể gửi OTP thanh toán."));
                    return;
                }
                otpSent = true;
                submitButton.setEnabled(true);
                codeInput.setText("");
                resultText.setText("Đã gửi OTP thanh toán tới email " + email + ".");
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                sendOtpButton.setEnabled(true);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void verifyAndSubmit() {
        String code = Ui.text(codeInput);
        if (!code.matches("\\d{6}")) {
            codeInput.setError("Mã xác thực phải gồm 6 chữ số");
            return;
        }
        if (!isOtp()) {
            if (!AppSession.hasPaymentPin(this)) {
                resultText.setText("Bạn cần thiết lập PIN thanh toán trước khi chuyển tiền.");
                Ui.open(this, PinActivity.class);
                return;
            }
            if (!AppSession.verifyPaymentPin(this, code)) {
                codeInput.setError("PIN không đúng");
                return;
            }
            submitTransfer();
            return;
        }
        if (!otpSent) {
            resultText.setText("Vui lòng gửi OTP thanh toán trước.");
            return;
        }
        submitButton.setEnabled(false);
        resultText.setText("Đang xác thực OTP thanh toán...");
        ApiClient.getApi().verifyPaymentOtp(new VerifyPaymentOtpRequest(AppSession.getUserEmail(this), code))
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (!response.isSuccessful()) {
                            submitButton.setEnabled(true);
                            resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể xác thực OTP thanh toán."));
                            return;
                        }
                        submitTransfer();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                        submitButton.setEnabled(true);
                        resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
                    }
                });
    }

    private void submitTransfer() {
        String from = value(TransferConfirmActivity.EXTRA_FROM);
        String to = value(TransferConfirmActivity.EXTRA_TO);
        BigDecimal amount = new BigDecimal(value(TransferConfirmActivity.EXTRA_AMOUNT));
        String note = value(TransferConfirmActivity.EXTRA_NOTE);
        resultText.setText("Đang xử lý chuyển tiền...");
        submitButton.setEnabled(false);
        ApiClient.getApi().transfer(new FundTransferRequest(from, to, amount, note))
                .enqueue(new Callback<FundTransferResponse>() {
                    @Override
                    public void onResponse(Call<FundTransferResponse> call, Response<FundTransferResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            submitButton.setEnabled(true);
                            resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể chuyển tiền."));
                            return;
                        }
                        refreshBalanceAndOpenSuccess(response.body());
                    }

                    @Override
                    public void onFailure(Call<FundTransferResponse> call, Throwable throwable) {
                        submitButton.setEnabled(true);
                        resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
                    }
                });
    }

    private void refreshBalanceAndOpenSuccess(FundTransferResponse transferResponse) {
        String from = value(TransferConfirmActivity.EXTRA_FROM);
        ApiClient.getApi().getBalance(from).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AppSession.saveAccountBalance(TransferAuthActivity.this, response.body());
                }
                openSuccess(transferResponse);
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                openSuccess(transferResponse);
            }
        });
    }

    private void openSuccess(FundTransferResponse response) {
        Intent intent = new Intent(this, TransferSuccessActivity.class);
        intent.putExtras(getIntent());
        intent.putExtra(TransferSuccessActivity.EXTRA_REFERENCE,
                response.transactionId == null ? "" : response.transactionId);
        startActivity(intent);
        finish();
    }

    private boolean isOtp() {
        return TransferConfirmActivity.AUTH_OTP.equals(value(TransferConfirmActivity.EXTRA_AUTH_METHOD));
    }

    private String value(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value.trim();
    }
}
