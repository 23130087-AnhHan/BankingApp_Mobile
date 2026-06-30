package com.example.bankingmobileapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MyQrActivity extends Activity {
    private ImageView qrImage;
    private TextView accountInfoText;
    private TextView resultText;
    private EditText amountInput;
    private EditText noteInput;
    private Button generateQrButton;
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0",
            DecimalFormatSymbols.getInstance(Locale.US));
    private boolean formattingAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }

        setContentView(R.layout.activity_my_qr);

        qrImage = findViewById(R.id.qrImage);
        accountInfoText = findViewById(R.id.accountInfoText);
        resultText = findViewById(R.id.resultText);
        amountInput = findViewById(R.id.amountInput);
        noteInput = findViewById(R.id.noteInput);
        generateQrButton = findViewById(R.id.generateQrButton);

        generateQrButton.setOnClickListener(v -> generateQr());
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable value) {
                if (formattingAmount) return;
                String digits = digitsOnly(value.toString());
                formattingAmount = true;
                if (digits.isEmpty()) {
                    amountInput.setText("");
                } else {
                    String formatted = moneyFormat.format(new BigDecimal(digits));
                    amountInput.setText(formatted);
                    amountInput.setSelection(formatted.length());
                }
                formattingAmount = false;
            }
        });

        renderAccountInfo();
        generateQr();
    }

    private void renderAccountInfo() {
        if (!AppSession.hasAccount(this)) {
            accountInfoText.setText("Bạn chưa có tài khoản thanh toán. Hãy quay lại dashboard để đồng bộ tài khoản.");
            generateQrButton.setEnabled(false);
            return;
        }
        if (!AppSession.isPaymentAccount(AppSession.getAccountType(this))) {
            accountInfoText.setText("Tài khoản hiện tại không phải PAYMENT_ACCOUNT. Hãy làm mới dashboard trước khi tạo QR.");
            generateQrButton.setEnabled(false);
            return;
        }

        accountInfoText.setText("Ngân hàng: " + TransferQrPayload.BANK_NAME
                + "\nSố tài khoản: " + AppSession.getAccountNumber(this)
                + "\nTên hiển thị: " + displayName());
        generateQrButton.setEnabled(true);
    }

    private void generateQr() {
        if (!generateQrButton.isEnabled()) {
            return;
        }
        try {
            BigDecimal amount = parseAmount();
            TransferQrPayload payload = TransferQrPayload.create(
                    AppSession.getAccountNumber(this),
                    displayName(),
                    amount,
                    Ui.text(noteInput));

            Bitmap bitmap = new BarcodeEncoder().encodeBitmap(
                    payload.toJson(),
                    BarcodeFormat.QR_CODE,
                    dp(280),
                    dp(280));
            qrImage.setImageBitmap(bitmap);
            resultText.setText(amount == null
                    ? "QR người nhận đã sẵn sàng. Người gửi quét QR này rồi nhập số tiền để chuyển."
                    : "QR thanh toán đã sẵn sàng. Người gửi quét QR này để tự điền người nhận, số tiền và nội dung.");
        } catch (NumberFormatException exception) {
            amountInput.setError("Số tiền không hợp lệ");
        } catch (JSONException exception) {
            resultText.setText("Không thể tạo dữ liệu QR: " + exception.getMessage());
        } catch (Exception exception) {
            resultText.setText("Không thể tạo QR. Vui lòng thử lại.");
        }
    }

    private BigDecimal parseAmount() {
        String digits = digitsOnly(Ui.text(amountInput));
        if (digits.isEmpty()) {
            return null;
        }
        BigDecimal amount = new BigDecimal(digits);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NumberFormatException("Amount must be positive");
        }
        return amount;
    }

    private String displayName() {
        String displayName = AppSession.getRememberedDisplayName(this);
        return displayName.isEmpty() ? "Khach hang NLU Banking" : displayName;
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
