package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.TransactionRequest;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionActivity extends Activity {
    private static final String TAG = "TransactionActivity";

    private EditText accountNumberInput;
    private EditText transactionTypeInput;
    private EditText amountInput;
    private EditText descriptionInput;
    private TextView resultText;
    private Button depositButton;
    private Button withdrawButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        accountNumberInput = findViewById(R.id.accountNumberInput);
        transactionTypeInput = findViewById(R.id.transactionTypeInput);
        amountInput = findViewById(R.id.amountInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        resultText = findViewById(R.id.resultText);
        depositButton = findViewById(R.id.depositButton);
        withdrawButton = findViewById(R.id.withdrawButton);

        transactionTypeInput.setText("DEPOSIT");
        if (AppSession.hasAccount(this)) {
            accountNumberInput.setText(AppSession.getAccountNumber(this));
            resultText.setText("Đã chọn tài khoản mặc định. Bạn chỉ cần nhập số tiền và nội dung.");
        } else {
            resultText.setText("Chưa có tài khoản mặc định. Hãy nhập số tài khoản hoặc mở tài khoản trước.");
        }

        depositButton.setOnClickListener(v -> submit("DEPOSIT"));
        withdrawButton.setOnClickListener(v -> submit("WITHDRAWAL"));
    }

    private void submit(String type) {
        String accountNumber = Ui.text(accountNumberInput);
        String amountValue = Ui.text(amountInput);
        if (accountNumber.isEmpty()) {
            accountNumberInput.setError("Vui lòng nhập số tài khoản");
            return;
        }
        if (amountValue.isEmpty()) {
            amountInput.setError("Vui lòng nhập số tiền");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountValue);
        } catch (NumberFormatException ex) {
            amountInput.setError("Số tiền không hợp lệ");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            amountInput.setError("Số tiền phải lớn hơn 0");
            return;
        }

        transactionTypeInput.setText(type);
        AppSession.saveAccountNumber(this, accountNumber);
        TransactionRequest request = new TransactionRequest(
                accountNumber,
                type,
                amount,
                Ui.text(descriptionInput)
        );

        resultText.setText("Đang xử lý giao dịch...");
        setActionsEnabled(false);
        ApiClient.getApi().createTransaction(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (!response.isSuccessful()) {
                    setActionsEnabled(true);
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể thực hiện giao dịch."));
                    return;
                }
                refreshBalance(accountNumber, "Giao dịch thành công\n" + Ui.formatBody(response.body()));
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                setActionsEnabled(true);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void refreshBalance(String accountNumber, String receipt) {
        ApiClient.getApi().getBalance(accountNumber).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                setActionsEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    if (!response.isSuccessful()) {
                        ApiErrorUtils.httpError(TAG, response, "Không thể tải số dư mới.");
                    }
                    resultText.setText(receipt + "\n\nChưa tải được số dư mới.");
                    return;
                }
                AppSession.saveAccountBalance(TransactionActivity.this, response.body());
                resultText.setText(receipt + "\n\nSố dư mới: " + CurrencyUtils.formatVnd(response.body()));
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                setActionsEnabled(true);
                ApiErrorUtils.networkError(TAG, throwable);
                resultText.setText(receipt + "\n\nGiao dịch đã hoàn tất nhưng chưa đồng bộ được số dư.");
            }
        });
    }

    private void setActionsEnabled(boolean enabled) {
        depositButton.setEnabled(enabled);
        withdrawButton.setEnabled(enabled);
    }
}
