package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.TransactionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends Activity {
    private static final String TAG = "HistoryActivity";

    private TextView currentAccountText;
    private TextView historyText;
    private Button loadHistoryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        currentAccountText = findViewById(R.id.currentAccountText);
        historyText = findViewById(R.id.historyText);
        loadHistoryButton = findViewById(R.id.loadHistoryButton);

        loadHistoryButton.setOnClickListener(v -> loadHistoryForCurrentAccount());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryForCurrentAccount();
    }

    private void loadHistoryForCurrentAccount() {
        if (!AppSession.hasAccount(this)) {
            currentAccountText.setText("Bạn chưa có tài khoản thanh toán.");
            historyText.setText("Chưa có tài khoản để xem lịch sử. Hãy quay lại dashboard để hệ thống cấp tài khoản.");
            loadHistoryButton.setEnabled(false);
            return;
        }
        if (!isSessionPaymentAccount()) {
            currentAccountText.setText("Tài khoản hiện tại không phải tài khoản thanh toán.");
            historyText.setText("Lịch sử giao dịch chỉ hiển thị cho tài khoản thanh toán chuẩn. Hãy làm mới dashboard để đồng bộ tài khoản.");
            loadHistoryButton.setEnabled(false);
            return;
        }

        String accountNumber = AppSession.getAccountNumber(this);
        currentAccountText.setText("STK: " + accountNumber);
        loadHistoryButton.setEnabled(true);
        loadHistory(accountNumber);
    }

    private void loadHistory(String accountNumber) {
        historyText.setText("Đang tải lịch sử giao dịch...");
        loadHistoryButton.setEnabled(false);
        // Query name is accountId, but the current Transaction-Service lookup uses account number.
        ApiClient.getApi().getTransactions(accountNumber).enqueue(new Callback<List<TransactionResponse>>() {
            @Override
            public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                loadHistoryButton.setEnabled(true);
                if (!response.isSuccessful()) {
                    historyText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể tải lịch sử giao dịch."));
                    return;
                }
                List<TransactionResponse> transactions = response.body();
                if (transactions == null || transactions.isEmpty()) {
                    historyText.setText("Tài khoản chưa có giao dịch nào.");
                    return;
                }

                StringBuilder builder = new StringBuilder();
                for (TransactionResponse item : transactions) {
                    if (item == null) {
                        continue;
                    }
                    builder.append(item.transactionType == null ? "GIAO DỊCH" : item.transactionType)
                            .append("    ")
                            .append(item.amount == null ? "0" : item.amount.toPlainString())
                            .append(" đ")
                            .append("\n")
                            .append(item.transactionStatus == null ? "Không rõ trạng thái" : item.transactionStatus)
                            .append("  •  ")
                            .append(item.localDateTime == null ? "Chưa có thời gian" : item.localDateTime)
                            .append("\nMã tham chiếu: ")
                            .append(item.referenceId == null ? "-" : item.referenceId)
                            .append("\n--------------------\n\n");
                }
                historyText.setText(builder.length() == 0
                        ? "Tài khoản chưa có giao dịch hợp lệ."
                        : builder.toString().trim());
            }

            @Override
            public void onFailure(Call<List<TransactionResponse>> call, Throwable throwable) {
                loadHistoryButton.setEnabled(true);
                historyText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private boolean isSessionPaymentAccount() {
        return AppSession.isPaymentAccount(AppSession.getAccountType(this));
    }
}
