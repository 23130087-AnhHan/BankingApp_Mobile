package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.model.TransactionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        EditText accountNumberInput = findViewById(R.id.accountNumberInput);
        TextView historyText = findViewById(R.id.historyText);
        accountNumberInput.setText(AppSession.getAccountNumber(this));

        findViewById(R.id.loadHistoryButton).setOnClickListener(v -> {
            String accountNumber = Ui.text(accountNumberInput);
            if (accountNumber.isEmpty()) {
                accountNumberInput.setError("Vui lòng nhập số tài khoản");
                historyText.setText("Nhập số tài khoản để xem lịch sử giao dịch.");
                return;
            }
            AppSession.saveAccountNumber(this, accountNumber);
            loadHistory(accountNumberInput, historyText);
        });
    }

    private void loadHistory(EditText accountNumberInput, TextView historyText) {
        historyText.setText("Loading transactions...");
        ApiClient.getApi().getTransactions(Ui.text(accountNumberInput)).enqueue(new Callback<List<TransactionResponse>>() {
            @Override
            public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                if (!response.isSuccessful()) {
                    historyText.setText("Load history failed. HTTP " + response.code());
                    return;
                }
                List<TransactionResponse> transactions = response.body();
                if (transactions == null || transactions.isEmpty()) {
                    historyText.setText("No transactions found.");
                    return;
                }

                StringBuilder builder = new StringBuilder();
                for (TransactionResponse item : transactions) {
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
                historyText.setText(builder.toString().trim());
            }

            @Override
            public void onFailure(Call<List<TransactionResponse>> call, Throwable throwable) {
                historyText.setText("Load history failed: " + throwable.getMessage());
            }
        });
    }
}
