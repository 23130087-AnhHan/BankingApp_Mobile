package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.TransactionResponse;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends Activity {
    private static final String TAG = "HistoryActivity";

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0",
            DecimalFormatSymbols.getInstance(Locale.US));

    private TextView currentAccountText;
    private TextView summaryText;
    private LinearLayout historyList;
    private Button loadHistoryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        currentAccountText = findViewById(R.id.currentAccountText);
        summaryText = findViewById(R.id.summaryText);
        historyList = findViewById(R.id.historyList);
        loadHistoryButton = findViewById(R.id.loadHistoryButton);

        findViewById(R.id.backHistoryButton).setOnClickListener(v -> finish());
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
            showMessage("Chưa có tài khoản để xem lịch sử. Hãy quay lại dashboard để hệ thống cấp tài khoản.");
            loadHistoryButton.setEnabled(false);
            return;
        }
        if (!isSessionPaymentAccount()) {
            currentAccountText.setText("Tài khoản hiện tại không phải tài khoản thanh toán.");
            showMessage("Lịch sử giao dịch chỉ hiển thị cho tài khoản thanh toán.");
            loadHistoryButton.setEnabled(false);
            return;
        }

        String accountNumber = AppSession.getAccountNumber(this);
        String balance = AppSession.getAccountBalance(this);
        currentAccountText.setText("STK: " + accountNumber
                + "\nSố dư hiện tại: " + formatMoney(balance) + " đ");
        loadHistoryButton.setEnabled(true);
        loadHistory(accountNumber);
    }

    private void loadHistory(String accountNumber) {
        showMessage("Đang tải lịch sử giao dịch...");
        loadHistoryButton.setEnabled(false);
        ApiClient.getApi().getTransactions(accountNumber).enqueue(new Callback<List<TransactionResponse>>() {
            @Override
            public void onResponse(Call<List<TransactionResponse>> call, Response<List<TransactionResponse>> response) {
                loadHistoryButton.setEnabled(true);
                if (!response.isSuccessful()) {
                    showMessage(ApiErrorUtils.httpError(TAG, response, "Không thể tải lịch sử giao dịch."));
                    return;
                }
                List<TransactionResponse> transactions = response.body();
                if (transactions == null || transactions.isEmpty()) {
                    showMessage("Tài khoản chưa có giao dịch nào.");
                    return;
                }
                renderTransactions(transactions);
            }

            @Override
            public void onFailure(Call<List<TransactionResponse>> call, Throwable throwable) {
                loadHistoryButton.setEnabled(true);
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void renderTransactions(List<TransactionResponse> transactions) {
        historyList.removeAllViews();
        int moneyInCount = 0;
        int moneyOutCount = 0;
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;

        for (TransactionResponse item : transactions) {
            if (item == null) {
                continue;
            }
            boolean moneyIn = isMoneyIn(item);
            BigDecimal amount = absoluteAmount(item);
            if (moneyIn) {
                moneyInCount++;
                totalIn = totalIn.add(amount);
            } else {
                moneyOutCount++;
                totalOut = totalOut.add(amount);
            }
            historyList.addView(createTransactionView(item, moneyIn, amount));
        }

        summaryText.setText("Tổng " + transactions.size() + " giao dịch"
                + "\nTiền vào: " + formatMoney(totalIn) + " đ (" + moneyInCount + " giao dịch)"
                + "\nTiền ra: " + formatMoney(totalOut) + " đ (" + moneyOutCount + " giao dịch)");
    }

    private View createTransactionView(TransactionResponse item, boolean moneyIn, BigDecimal amount) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.nlu_list_card_bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        card.setClickable(true);
        card.setOnClickListener(v -> openTransactionDetail(item));

        TextView title = new TextView(this);
        title.setText(firstNonEmpty(item.displayTitle, moneyIn ? "Nhận tiền" : "Chuyển tiền"));
        title.setTextColor(Color.parseColor("#101828"));
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView amountText = new TextView(this);
        amountText.setText((moneyIn ? "+ " : "- ") + formatMoney(amount) + " đ");
        amountText.setTextColor(Color.parseColor(moneyIn ? "#11845B" : "#C62828"));
        amountText.setTextSize(22);
        amountText.setTypeface(Typeface.DEFAULT_BOLD);
        amountText.setPadding(0, dp(6), 0, dp(8));
        card.addView(amountText);

        TextView message = new TextView(this);
        message.setText(buildMessage(item, moneyIn));
        message.setTextColor(Color.parseColor("#344054"));
        message.setTextSize(14);
        message.setLineSpacing(dp(2), 1.0f);
        card.addView(message);

        TextView meta = new TextView(this);
        meta.setText("Trạng thái: " + displayStatus(item.transactionStatus)
                + "\nThời gian: " + firstNonEmpty(item.localDateTime, "Chưa có thời gian")
                + "\nMã giao dịch: " + firstNonEmpty(item.referenceId, "-"));
        meta.setTextColor(Color.parseColor("#667085"));
        meta.setTextSize(13);
        meta.setPadding(0, dp(10), 0, 0);
        meta.setLineSpacing(dp(2), 1.0f);
        card.addView(meta);

        TextView action = new TextView(this);
        action.setText("Xem biên lai");
        action.setTextColor(Color.parseColor("#075B35"));
        action.setTextSize(14);
        action.setTypeface(Typeface.DEFAULT_BOLD);
        action.setPadding(0, dp(10), 0, 0);
        card.addView(action);

        return card;
    }

    private void openTransactionDetail(TransactionResponse item) {
        if (item == null || item.referenceId == null || item.referenceId.trim().isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, TransactionDetailActivity.class);
        intent.putExtra("referenceId", item.referenceId);
        if (item.transactionId != null) {
            intent.putExtra("transactionId", item.transactionId);
        }
        startActivity(intent);
    }

    private String buildMessage(TransactionResponse item, boolean moneyIn) {
        String counterparty = firstNonEmpty(item.counterpartyAccount, "");
        String base = firstNonEmpty(item.displayMessage, moneyIn ? "Tiền vào tài khoản" : "Tiền ra khỏi tài khoản");
        String bank = firstNonEmpty(item.bankName, "NLU Banking");
        String note = firstNonEmpty(item.comments, "");

        StringBuilder builder = new StringBuilder(base);
        if (!counterparty.isEmpty()) {
            builder.append("\nTài khoản đối ứng: ").append(counterparty);
        }
        builder.append("\nNgân hàng: ").append(bank);
        if (!note.isEmpty()) {
            builder.append("\nNội dung: ").append(note);
        }
        return builder.toString();
    }

    private void showMessage(String message) {
        summaryText.setText(message);
        historyList.removeAllViews();
    }

    private boolean isMoneyIn(TransactionResponse item) {
        if ("IN".equalsIgnoreCase(item.direction)) {
            return true;
        }
        if ("OUT".equalsIgnoreCase(item.direction)) {
            return false;
        }
        BigDecimal value = item.signedAmount != null ? item.signedAmount : item.amount;
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal absoluteAmount(TransactionResponse item) {
        BigDecimal value = item.amount != null ? item.amount : item.signedAmount;
        return value == null ? BigDecimal.ZERO : value.abs();
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

    private String formatMoney(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "0";
        }
        try {
            return moneyFormat.format(new BigDecimal(rawValue.replace(",", "").replace("đ", "").trim()));
        } catch (NumberFormatException exception) {
            return rawValue;
        }
    }

    private String formatMoney(BigDecimal amount) {
        return moneyFormat.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private String firstNonEmpty(String first, String fallback) {
        return first == null || first.trim().isEmpty() ? fallback : first.trim();
    }

    private boolean isSessionPaymentAccount() {
        return "PAYMENT_ACCOUNT".equalsIgnoreCase(AppSession.getAccountType(this));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
