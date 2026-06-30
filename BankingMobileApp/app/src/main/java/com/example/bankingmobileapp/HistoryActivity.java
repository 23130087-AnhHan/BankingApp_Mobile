package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

    private EditText accountNumberInput;
    private TextView historyText;
    private Button loadHistoryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        accountNumberInput = findViewById(R.id.accountNumberInput);
        historyText = findViewById(R.id.historyText);
        loadHistoryButton = findViewById(R.id.loadHistoryButton);

        if (AppSession.hasAccount(this)) {
            String accountNumber = AppSession.getAccountNumber(this);
            accountNumberInput.setText(accountNumber);
            loadHistory(accountNumber);
        } else {
            historyText.setText("Chưa có tài khoản để xem lịch sử. Hãy mở hoặc chọn tài khoản trước.");
        }

        loadHistoryButton.setOnClickListener(v -> {
            String accountNumber = Ui.text(accountNumberInput);
            if (accountNumber.isEmpty()) {
                accountNumberInput.setError("Vui lòng nhập số tài khoản");
                historyText.setText("Chưa có tài khoản để xem lịch sử.");
                return;
            }
            AppSession.saveAccountNumber(this, accountNumber);
            loadHistory(accountNumber);
        });
    }

    private void loadHistory(String accountNumber) {
        historyText.setText("Đang tải lịch sử giao dịch...");
        loadHistoryButton.setEnabled(false);
        // Query có tên accountId nhưng luồng /transactions hiện lưu và tra cứu bằng accountNumber.
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
                            .append(" ₫")
                            .append("\n")
                            .append(item.transactionStatus == null ? "Không rõ trạng thái" : item.transactionStatus)
                            .append("  •  ")
                            .append(item.localDateTime == null ? "Chưa có thời gian" : item.localDateTime)
                            .append("\nMã tham chiếu: ")
                            .append(item.referenceId == null ? "—" : item.referenceId)
                            .append("\n────────────────────\n\n");
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
}
