package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PinActivity extends Activity {
    private EditText currentPinInput;
    private EditText newPinInput;
    private EditText confirmPinInput;
    private TextView statusText;
    private Button savePinButton;
    private Button clearPinButton;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }

        setContentView(R.layout.activity_pin);

        currentPinInput = findViewById(R.id.currentPinInput);
        newPinInput = findViewById(R.id.newPinInput);
        confirmPinInput = findViewById(R.id.confirmPinInput);
        statusText = findViewById(R.id.statusText);
        savePinButton = findViewById(R.id.savePinButton);
        clearPinButton = findViewById(R.id.clearPinButton);
        backButton = findViewById(R.id.backButton);

        configurePinInput(currentPinInput);
        configurePinInput(newPinInput);
        configurePinInput(confirmPinInput);

        savePinButton.setOnClickListener(v -> savePin());
        clearPinButton.setOnClickListener(v -> clearPin());
        backButton.setOnClickListener(v -> finish());
        renderState();
    }

    private void configurePinInput(EditText input) {
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
    }

    private void renderState() {
        boolean hasPin = AppSession.hasPaymentPin(this);
        currentPinInput.setEnabled(hasPin);
        currentPinInput.setHint(hasPin ? "Nhập PIN hiện tại" : "Chưa có PIN");
        clearPinButton.setEnabled(hasPin);
        statusText.setText(hasPin
                ? "PIN thanh toán đang được bật. Bạn có thể đổi PIN hoặc xóa PIN hiện tại."
                : "Bạn chưa có PIN thanh toán. Hãy thiết lập PIN 6 số trước khi chuyển tiền bằng PIN.");
    }

    private void savePin() {
        String currentPin = Ui.text(currentPinInput);
        String newPin = Ui.text(newPinInput);
        String confirmPin = Ui.text(confirmPinInput);

        if (AppSession.hasPaymentPin(this) && !AppSession.verifyPaymentPin(this, currentPin)) {
            currentPinInput.setError("PIN hiện tại không đúng");
            return;
        }
        if (!isValidPin(newPin)) {
            newPinInput.setError("PIN phải gồm đúng 6 chữ số");
            return;
        }
        if (!newPin.equals(confirmPin)) {
            confirmPinInput.setError("PIN xác nhận không khớp");
            return;
        }

        AppSession.savePaymentPin(this, newPin);
        currentPinInput.setText("");
        newPinInput.setText("");
        confirmPinInput.setText("");
        renderState();
        statusText.setText("Đã cập nhật PIN thanh toán.");
    }

    private void clearPin() {
        String currentPin = Ui.text(currentPinInput);
        if (!AppSession.verifyPaymentPin(this, currentPin)) {
            currentPinInput.setError("PIN hiện tại không đúng");
            return;
        }
        AppSession.clearPaymentPin(this);
        currentPinInput.setText("");
        newPinInput.setText("");
        confirmPinInput.setText("");
        renderState();
        statusText.setText("Đã xóa PIN thanh toán.");
    }

    private boolean isValidPin(String pin) {
        return pin != null && pin.matches("\\d{6}");
    }
}
