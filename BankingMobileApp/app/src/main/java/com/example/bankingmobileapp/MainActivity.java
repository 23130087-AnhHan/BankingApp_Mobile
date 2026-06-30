package com.example.bankingmobileapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.AccountRequest;
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.NotificationResponse;
import com.example.bankingmobileapp.model.RefreshTokenRequest;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.math.BigDecimal;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private TextView accountNumberText;
    private TextView balanceText;
    private TextView statusText;
    private TextView userIdText;
    private TextView userNameText;
    private TextView notificationBadge;
    private ImageButton toggleBalanceButton;
    private String formattedBalance = CurrencyUtils.formatVnd("0");
    private boolean balanceVisible;
    private boolean provisioningAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            openLoginAndFinish();
            return;
        }

        setContentView(R.layout.activity_main);

        accountNumberText = findViewById(R.id.accountNumberText);
        balanceText = findViewById(R.id.balanceText);
        statusText = findViewById(R.id.statusText);
        userIdText = findViewById(R.id.userIdText);
        userNameText = findViewById(R.id.userNameText);
        toggleBalanceButton = findViewById(R.id.toggleBalanceButton);
        notificationBadge = findViewById(R.id.notificationBadge);
        findViewById(R.id.accountTile).setOnClickListener(v -> Ui.open(this, AccountActivity.class));
        findViewById(R.id.transferTile).setOnClickListener(v -> Ui.open(this, TransferActivity.class));
        findViewById(R.id.scanQrTile).setOnClickListener(v -> scanTransferQr());
        findViewById(R.id.historyTile).setOnClickListener(v -> Ui.open(this, HistoryActivity.class));
        findViewById(R.id.beneficiaryTile).setOnClickListener(v -> Ui.open(this, BeneficiaryActivity.class));
        findViewById(R.id.myQrTile).setOnClickListener(v -> Ui.open(this, MyQrActivity.class));
        findViewById(R.id.notificationTile).setOnClickListener(v -> Ui.open(this, NotificationActivity.class));
        findViewById(R.id.productTile).setOnClickListener(v -> Toast.makeText(
                this, "Tính năng sản phẩm đang được phát triển", Toast.LENGTH_SHORT).show());
        findViewById(R.id.copyAccountButton).setOnClickListener(v -> copyAccountNumber());
        toggleBalanceButton.setOnClickListener(v -> toggleBalanceVisibility());
        findViewById(R.id.logoutButton).setOnClickListener(v -> confirmLogout());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                openTransferFromQr(scanResult.getContents());
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AppSession.hasValidSession(this)) {
            openLoginAndFinish();
        } else {
            refreshPaymentAccount();
            loadUnreadNotificationCount();
        }
    }

    private void refreshPaymentAccount() {
        renderSessionSnapshot();
        if (!AppSession.hasUser(this)) {
            userIdText.setText(AppSession.getUserEmail(this).isEmpty()
                    ? "Chưa có khách hàng"
                    : AppSession.getUserEmail(this));
            statusText.setText(AppSession.hasAccount(this) ? "Đã có tài khoản" : "Chưa có tài khoản");
            return;
        }

        Long userId = currentUserId();
        if (userId == null) {
            userIdText.setText("Phiên đăng nhập hết hạn");
            statusText.setText("Cần đăng nhập lại");
            return;
        }

        userIdText.setText(AppSession.getUserEmail(this).isEmpty()
                ? "Khách hàng #" + userId
                : AppSession.getUserEmail(this));
        statusText.setText("Đang đồng bộ...");
        ApiClient.getApi().getAccountByUserId(userId).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountResponse account = response.body();
                    if (!AppSession.isPaymentAccount(account.accountType)) {
                        provisionDefaultPaymentAccount(userId);
                        return;
                    }
                    saveAndRenderAccount(account);
                } else if (response.code() == 404 || response.code() == 400) {
                    // Try to create if not found
                    provisionDefaultPaymentAccount(userId);
                } else {
                    statusText.setText("Không thể đồng bộ");
                    ApiErrorUtils.httpError(TAG, response, "Lỗi kết nối tài khoản.");
                }
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable throwable) {
                statusText.setText("Offline");
                renderSessionSnapshot();
            }
        });
    }

    private void scanTransferQr() {
        if (!AppSession.hasAccount(this) || !AppSession.isPaymentAccount(AppSession.getAccountType(this))) {
            statusText.setText("Cần tài khoản thanh toán trước khi quét QR");
            refreshPaymentAccount();
            return;
        }
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Quét QR nhận tiền NLU Banking");
        integrator.setBeepEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    private void openTransferFromQr(String rawQrValue) {
        try {
            TransferQrPayload payload = TransferQrPayload.parse(rawQrValue);
            if (payload.accountNumber.equals(AppSession.getAccountNumber(this))) {
                statusText.setText("Không thể chuyển cho chính tài khoản của bạn");
                return;
            }

            Intent intent = new Intent(this, TransferActivity.class);
            intent.putExtra(TransferActivity.EXTRA_QR_ACCOUNT_NUMBER, payload.accountNumber);
            intent.putExtra(TransferActivity.EXTRA_QR_ACCOUNT_HOLDER_NAME, payload.accountHolderName);
            if (payload.amount != null) {
                intent.putExtra(TransferActivity.EXTRA_QR_AMOUNT, payload.amount.stripTrailingZeros().toPlainString());
            }
            intent.putExtra(TransferActivity.EXTRA_QR_NOTE, payload.note);
            startActivity(intent);
        } catch (Exception exception) {
            statusText.setText("QR không hợp lệ");
        }
    }

    private void provisionDefaultPaymentAccount(long userId) {
        if (provisioningAccount) return;
        provisioningAccount = true;
        statusText.setText("Đang mở tài khoản...");

        AccountRequest request = new AccountRequest("PAYMENT_ACCOUNT", BigDecimal.ZERO, userId);
        ApiClient.getApi().createAccount(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                provisioningAccount = false;
                // If success or already exists (409), try to fetch it again
                if (response.isSuccessful() || response.code() == 409) {
                    refreshPaymentAccount();
                } else {
                    statusText.setText("Lỗi cấp tài khoản");
                    ApiErrorUtils.httpError(TAG, response, "Không thể mở tài khoản mới.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                provisioningAccount = false;
                statusText.setText("Lỗi kết nối");
            }
        });
    }

    private void renderProvisioningState() {
        setBalance("0");
        accountNumberText.setText("Đang cấp tài khoản thanh toán");
        statusText.setText("Đang mở tài khoản");
    }

    private void renderSessionSnapshot() {
        String displayName = AppSession.getRememberedDisplayName(this);
        userNameText.setText(displayName.isEmpty() ? "Khách hàng NLU" : displayName);
        String accountNumber = AppSession.getAccountNumber(this);
        String balance = AppSession.getAccountBalance(this);
        accountNumberText.setText(accountNumber.isEmpty()
                ? "Đang kiểm tra tài khoản"
                : accountNumber);
        setBalance(balance);
    }

    private void saveAndRenderAccount(AccountResponse account) {
        if (account == null || !AppSession.isPaymentAccount(account.accountType)) {
            AppSession.clearAccount(this);
            statusText.setText("Cần tài khoản thanh toán");
            accountNumberText.setText(account == null ? "Không tìm thấy tài khoản" : "Tài khoản hiện tại không phải tài khoản thanh toán");
            setBalance("0");
            return;
        }

        String accountNumber = account.accountNumber == null ? "" : account.accountNumber.trim();
        if (accountNumber.isEmpty()) {
            accountNumberText.setText("Chưa có số tài khoản");
            setBalance("0");
            statusText.setText("Thiếu số tài khoản");
            return;
        }

        AppSession.saveAccount(this, account);
        String balance = account.availableBalance == null ? "0" : account.availableBalance.toPlainString();

        accountNumberText.setText(accountNumber);
        setBalance(balance);
        statusText.setText(formatAccountStatus(account.accountStatus));
    }

    private void copyAccountNumber() {
        String accountNumber = AppSession.getAccountNumber(this);
        if (accountNumber.isEmpty()) {
            Toast.makeText(this, "Chưa có số tài khoản để sao chép", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Số tài khoản NLU Bank", accountNumber));
        Toast.makeText(this, "Đã sao chép số tài khoản", Toast.LENGTH_SHORT).show();
    }

    private void toggleBalanceVisibility() {
        balanceVisible = !balanceVisible;
        renderBalanceVisibility();
    }

    private void setBalance(String balance) {
        formattedBalance = CurrencyUtils.formatVnd(balance);
        renderBalanceVisibility();
    }

    private void renderBalanceVisibility() {
        balanceText.setText(balanceVisible ? formattedBalance : "********");
        toggleBalanceButton.setImageResource(
                balanceVisible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
        toggleBalanceButton.setContentDescription(balanceVisible ? "Ẩn số dư" : "Hiện số dư");
    }

    private boolean isPaymentAccount(AccountResponse account) {
        return account != null && AppSession.isPaymentAccount(account.accountType);
    }

    private Long currentUserId() {
        try {
            return Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatAccountStatus(String status) {
        if ("ACTIVE".equals(status)) {
            return "Đang hoạt động";
        }
        if ("PENDING".equals(status)) {
            return "Đang chờ xử lý";
        }
        if ("BLOCKED".equals(status)) {
            return "Đang bị khóa";
        }
        if ("CLOSED".equals(status)) {
            return "Đã đóng";
        }
        return status == null || status.trim().isEmpty() ? "Không rõ trạng thái" : status;
    }

    private void confirmLogout() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_logout, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.cancelLogoutButton).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.confirmLogoutButton).setOnClickListener(v -> {
            dialog.dismiss();
            logout();
        });

        dialog.setOnShowListener(d -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialog.show();
    }

    private void logout() {
        String refreshToken = AppSession.getRefreshToken(this);
        if (refreshToken.isEmpty()) {
            finishLogout();
            return;
        }
        ApiClient.getApi().logout(new RefreshTokenRequest(refreshToken)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                finishLogout();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                finishLogout();
            }
        });
    }

    private void finishLogout() {
        AppSession.clearLoginState(this);
        Ui.openAndClear(this, WelcomeActivity.class);
    }

    private void openLoginAndFinish() {
        Ui.openAndClear(this, WelcomeActivity.class);
    }

    private void loadUnreadNotificationCount() {
        Long userId = currentUserId();
        if (userId == null) return;
        ApiClient.getApi().getNotifications(userId).enqueue(new Callback<List<NotificationResponse>>() {
            @Override
            public void onResponse(Call<List<NotificationResponse>> call, Response<List<NotificationResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    notificationBadge.setVisibility(View.GONE);
                    return;
                }
                int unread = 0;
                for (NotificationResponse n : response.body()) {
                    if (n.read == null || !n.read) {
                        unread++;
                    }
                }
                if (unread > 0) {
                    notificationBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                    notificationBadge.setVisibility(View.VISIBLE);
                } else {
                    notificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<NotificationResponse>> call, Throwable t) {
                notificationBadge.setVisibility(View.GONE);
            }
        });
    }
}
