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
import com.example.bankingmobileapp.model.NotificationResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends Activity {
    private static final String TAG = "NotificationActivity";

    private TextView summaryText;
    private LinearLayout notificationList;
    private Button refreshButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        summaryText = findViewById(R.id.notificationSummaryText);
        notificationList = findViewById(R.id.notificationList);
        refreshButton = findViewById(R.id.refreshNotificationButton);
        refreshButton.setOnClickListener(v -> loadNotifications());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        Long userId = currentUserId();
        if (userId == null) {
            showMessage("Phiên đăng nhập chưa có mã khách hàng. Vui lòng đăng nhập lại.");
            return;
        }
        refreshButton.setEnabled(false);
        showMessage("Đang tải thông báo...");
        ApiClient.getApi().getNotifications(userId).enqueue(new Callback<List<NotificationResponse>>() {
            @Override
            public void onResponse(Call<List<NotificationResponse>> call, Response<List<NotificationResponse>> response) {
                refreshButton.setEnabled(true);
                if (!response.isSuccessful()) {
                    showMessage(ApiErrorUtils.httpError(TAG, response, "Không thể tải thông báo."));
                    return;
                }
                renderNotifications(response.body());
            }

            @Override
            public void onFailure(Call<List<NotificationResponse>> call, Throwable throwable) {
                refreshButton.setEnabled(true);
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void renderNotifications(List<NotificationResponse> notifications) {
        notificationList.removeAllViews();
        if (notifications == null || notifications.isEmpty()) {
            summaryText.setText("Bạn chưa có thông báo nào.");
            return;
        }
        int unread = 0;
        for (NotificationResponse notification : notifications) {
            if (notification != null && !isRead(notification)) {
                unread++;
            }
        }
        summaryText.setText("Tổng " + notifications.size() + " thông báo"
                + "\nChưa đọc: " + unread);
        for (NotificationResponse notification : notifications) {
            if (notification != null) {
                notificationList.addView(createNotificationView(notification));
            }
        }
    }

    private View createNotificationView(NotificationResponse notification) {
        boolean unread = !isRead(notification);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.panel_bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);
        card.setClickable(true);
        card.setOnClickListener(v -> handleNotificationClick(notification));

        TextView status = new TextView(this);
        status.setText(unread ? "Chưa đọc" : "Đã đọc");
        status.setTextColor(Color.parseColor(unread ? "#0F6BFF" : "#667085"));
        status.setTextSize(12);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(status);

        TextView title = new TextView(this);
        title.setText(firstNonEmpty(notification.title, "Thông báo"));
        title.setTextColor(Color.parseColor("#101828"));
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(6), 0, 0);
        card.addView(title);

        TextView message = new TextView(this);
        message.setText(firstNonEmpty(notification.message, ""));
        message.setTextColor(Color.parseColor("#344054"));
        message.setTextSize(14);
        message.setPadding(0, dp(8), 0, 0);
        message.setLineSpacing(dp(2), 1.0f);
        card.addView(message);

        TextView meta = new TextView(this);
        meta.setText(displayType(notification.type)
                + "\nThời gian: " + firstNonEmpty(notification.createdAt, "-")
                + (hasReference(notification) ? "\nMã giao dịch: " + notification.referenceId.trim() : ""));
        meta.setTextColor(Color.parseColor("#667085"));
        meta.setTextSize(13);
        meta.setPadding(0, dp(10), 0, 0);
        card.addView(meta);

        if (hasReference(notification)) {
            TextView action = new TextView(this);
            action.setText("Xem biên lai");
            action.setTextColor(Color.parseColor("#0F6BFF"));
            action.setTextSize(14);
            action.setTypeface(Typeface.DEFAULT_BOLD);
            action.setPadding(0, dp(10), 0, 0);
            card.addView(action);
        }
        return card;
    }

    private void handleNotificationClick(NotificationResponse notification) {
        markAsRead(notification);
        if (hasReference(notification)) {
            Intent intent = new Intent(this, TransactionDetailActivity.class);
            intent.putExtra("referenceId", notification.referenceId.trim());
            startActivity(intent);
        }
    }

    private void markAsRead(NotificationResponse notification) {
        Long userId = currentUserId();
        if (userId == null || notification.id == null || isRead(notification)) {
            return;
        }
        ApiClient.getApi().markNotificationRead(userId, notification.id).enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Call<NotificationResponse> call, Response<NotificationResponse> response) {
                if (response.isSuccessful()) {
                    loadNotifications();
                }
            }

            @Override
            public void onFailure(Call<NotificationResponse> call, Throwable throwable) {
                ApiErrorUtils.networkError(TAG, throwable);
            }
        });
    }

    private void showMessage(String message) {
        summaryText.setText(message);
        notificationList.removeAllViews();
    }

    private boolean isRead(NotificationResponse notification) {
        return notification.read != null && notification.read;
    }

    private boolean hasReference(NotificationResponse notification) {
        return notification.referenceId != null && !notification.referenceId.trim().isEmpty();
    }

    private String displayType(String type) {
        if ("TRANSFER_OUT".equalsIgnoreCase(type)) {
            return "Biến động số dư: tiền ra";
        }
        if ("TRANSFER_IN".equalsIgnoreCase(type)) {
            return "Biến động số dư: tiền vào";
        }
        if ("TRANSFER_FAILED".equalsIgnoreCase(type)) {
            return "Giao dịch thất bại";
        }
        return firstNonEmpty(type, "Thông báo hệ thống");
    }

    private Long currentUserId() {
        try {
            return Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String firstNonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
