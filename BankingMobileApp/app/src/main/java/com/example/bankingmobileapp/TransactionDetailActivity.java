package com.example.bankingmobileapp;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.TransactionResponse;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionDetailActivity extends Activity {
    private static final String TAG = "TransactionDetailActivity";

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0",
            DecimalFormatSymbols.getInstance(Locale.US));

    private TextView statusText;
    private TextView amountText;
    private TextView messageText;
    private LinearLayout receiptRows;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }

        setContentView(R.layout.activity_transaction_detail);

        statusText = findViewById(R.id.receiptStatusText);
        amountText = findViewById(R.id.receiptAmountText);
        messageText = findViewById(R.id.receiptMessageText);
        receiptRows = findViewById(R.id.receiptRows);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());
        loadReceipt();
    }

    private void loadReceipt() {
        String referenceId = getIntent().getStringExtra("referenceId");
        if (referenceId == null || referenceId.trim().isEmpty()) {
            showError("Không tìm thấy mã giao dịch để tải biên lai.");
            return;
        }

        messageText.setText("Đang tải chi tiết giao dịch...");
        ApiClient.getApi().getTransactionReceipt(referenceId).enqueue(new Callback<TransactionResponse>() {
            @Override
            public void onResponse(Call<TransactionResponse> call, Response<TransactionResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showError(ApiErrorUtils.httpError(TAG, response, "Không thể tải biên lai giao dịch."));
                    return;
                }
                renderReceipt(response.body());
            }

            @Override
            public void onFailure(Call<TransactionResponse> call, Throwable throwable) {
                showError(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void renderReceipt(TransactionResponse receipt) {
        boolean moneyIn = "IN".equalsIgnoreCase(receipt.direction);
        statusText.setText(displayStatus(firstNonEmpty(receipt.status, receipt.transactionStatus)));
        statusText.setTextColor(Color.parseColor("#11845B"));
        amountText.setText((moneyIn ? "+ " : "- ") + formatMoney(receipt.amount) + " đ");
        amountText.setTextColor(Color.parseColor(moneyIn ? "#11845B" : "#C62828"));
        messageText.setText(moneyIn ? "Tiền đã vào tài khoản" : "Chuyển tiền thành công");

        receiptRows.removeAllViews();
        addRow("Từ tài khoản", firstNonEmpty(receipt.fromAccount, "-"));
        addRow("Đến tài khoản", firstNonEmpty(receipt.toAccount, "-"));
        addRow("Tên người nhận", firstNonEmpty(receipt.recipientName, receipt.counterpartyName));
        addRow("Ngân hàng nhận", firstNonEmpty(receipt.recipientBank, firstNonEmpty(receipt.bankName, "NLU Banking")));
        addRow("Nội dung", firstNonEmpty(receipt.description, firstNonEmpty(receipt.comments, "Không có")));
        addRow("Mã giao dịch", firstNonEmpty(receipt.referenceId, "-"));
        addRow("Thời gian", firstNonEmpty(receipt.time, firstNonEmpty(receipt.localDateTime, "-")));
        addRow("Loại giao dịch", displayTransactionType(firstNonEmpty(receipt.type, firstNonEmpty(receipt.transactionType, "-"))));
    }

    private String displayTransactionType(String type) {
        if (type == null || type.trim().isEmpty()) return "-";
        String upper = type.toUpperCase().trim();
        switch (upper) {
            case "INTERNAL_TRANSFER":
            case "TRANSFER":
                return "Chuyển tiền";
            case "CASH_DEPOSIT":
            case "DEPOSIT":
                return "Nạp tiền";
            case "WITHDRAWAL":
                return "Rút tiền";
            case "UTILITY_BILL":
            case "BILL_PAYMENT":
                return "Thanh toán hóa đơn";
            default:
                return type;
        }
    }

    private void addRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.parseColor("#667085"));
        labelView.setTextSize(13);
        row.addView(labelView);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(Color.parseColor("#101828"));
        valueView.setTextSize(16);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setPadding(0, dp(3), 0, 0);
        row.addView(valueView);

        receiptRows.addView(row);
    }

    private void showError(String message) {
        statusText.setText("Không tải được");
        amountText.setText("--");
        String referenceId = getIntent().getStringExtra("referenceId");
        StringBuilder builder = new StringBuilder();
        builder.append(message == null || message.trim().isEmpty()
                ? "Không thể tải biên lai giao dịch."
                : message.trim());
        if (referenceId != null && !referenceId.trim().isEmpty()) {
            builder.append("\nMã giao dịch: ").append(referenceId.trim());
        }
        if (!builder.toString().contains("Transaction-Service")) {
            builder.append("\nVui lòng kiểm tra Transaction-Service, Account-Service và thử lại.");
        }
        messageText.setText(builder.toString());
        receiptRows.removeAllViews();
    }

    private String displayStatus(String status) {
        if ("COMPLETED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            return "Thành công";
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            return "Đang xử lý";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "Thất bại";
        }
        return firstNonEmpty(status, "Không rõ");
    }

    private String formatMoney(BigDecimal amount) {
        return moneyFormat.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private String firstNonEmpty(String first, String fallback) {
        return first == null || first.trim().isEmpty() ? fallback : first.trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
