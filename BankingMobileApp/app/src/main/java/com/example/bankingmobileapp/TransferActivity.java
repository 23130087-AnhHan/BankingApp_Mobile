package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.FundTransferRequest;
import com.example.bankingmobileapp.model.FundTransferResponse;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferActivity extends Activity {
    private static final String TAG = "TransferActivity";
    private static final String INTERNAL_BANK = "NLU Banking";

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0",
            DecimalFormatSymbols.getInstance(Locale.US));

    private TextView sourceAccountText;
    private Spinner bankSpinner;
    private EditText toAccountInput;
    private EditText amountInput;
    private EditText transferNoteInput;
    private TextView resultText;
    private TextView confirmSummaryText;
    private View confirmationPanel;
    private RadioButton otpEmailOption;
    private RadioButton pinOption;
    private EditText pinInput;
    private Button transferButton;
    private Button submitButton;
    private Button editButton;
    private Button managePinButton;

    private String pendingFromAccount;
    private String pendingToBank;
    private String pendingToAccount;
    private BigDecimal pendingAmount;
    private String pendingNote;
    private boolean formattingAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        sourceAccountText = findViewById(R.id.sourceAccountText);
        bankSpinner = findViewById(R.id.bankSpinner);
        toAccountInput = findViewById(R.id.toAccountInput);
        amountInput = findViewById(R.id.amountInput);
        transferNoteInput = findViewById(R.id.transferNoteInput);
        resultText = findViewById(R.id.resultText);
        confirmSummaryText = findViewById(R.id.confirmSummaryText);
        confirmationPanel = findViewById(R.id.confirmationPanel);
        otpEmailOption = findViewById(R.id.otpEmailOption);
        pinOption = findViewById(R.id.pinOption);
        pinInput = findViewById(R.id.pinInput);
        transferButton = findViewById(R.id.transferButton);
        submitButton = findViewById(R.id.submitButton);
        editButton = findViewById(R.id.editButton);
        managePinButton = findViewById(R.id.managePinButton);

        setupBankSpinner();
        setupAmountInput();
        setupPinInput();

        transferButton.setOnClickListener(v -> validateAndShowConfirmation());
        submitButton.setOnClickListener(v -> verifyAndSubmit());
        editButton.setOnClickListener(v -> hideConfirmation());
        managePinButton.setOnClickListener(v -> Ui.open(this, PinActivity.class));

        renderSourceAccount();
        hideConfirmation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderSourceAccount();
        renderPinState();
    }

    private void setupBankSpinner() {
        String[] banks = {
                INTERNAL_BANK,
                "Vietcombank",
                "BIDV",
                "VietinBank",
                "Techcombank",
                "MB Bank"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, banks);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bankSpinner.setAdapter(adapter);
    }

    private void setupAmountInput() {
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable value) {
                if (formattingAmount) {
                    return;
                }
                String digits = digitsOnly(value.toString());
                formattingAmount = true;
                if (digits.isEmpty()) {
                    amountInput.setText("");
                } else {
                    amountInput.setText(moneyFormat.format(new BigDecimal(digits)));
                    amountInput.setSelection(amountInput.getText().length());
                }
                formattingAmount = false;
                hideConfirmation();
            }
        });
    }

    private void setupPinInput() {
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
    }

    private void renderSourceAccount() {
        if (!AppSession.hasAccount(this)) {
            sourceAccountText.setText("Bạn chưa có tài khoản thanh toán. Hãy quay lại dashboard để hệ thống cấp tài khoản.");
            resultText.setText("Không thể chuyển tiền khi chưa có tài khoản nguồn.");
            setTransferEnabled(false);
            return;
        }
        if (!isSessionPaymentAccount()) {
            sourceAccountText.setText("Tài khoản nguồn hiện tại không phải tài khoản thanh toán.");
            resultText.setText("Chuyển tiền chỉ dùng PAYMENT_ACCOUNT. Hãy làm mới dashboard để đồng bộ tài khoản chuẩn.");
            setTransferEnabled(false);
            return;
        }

        String accountNumber = AppSession.getAccountNumber(this);
        String balance = AppSession.getAccountBalance(this);
        sourceAccountText.setText("STK nguồn: " + accountNumber
                + "\nSố dư khả dụng: " + CurrencyUtils.formatVnd(balance));
        setTransferEnabled(true);
    }

    private void renderPinState() {
        boolean hasPin = AppSession.hasPaymentPin(this);
        pinOption.setChecked(true);
        pinInput.setEnabled(hasPin);
        submitButton.setEnabled(hasPin);
        managePinButton.setVisibility(hasPin ? View.GONE : View.VISIBLE);
        if (!hasPin) {
            pinInput.setText("");
            pinInput.setHint("Bạn cần thiết lập PIN trước");
        } else {
            pinInput.setHint("Nhập PIN 6 số");
        }
    }

    private void validateAndShowConfirmation() {
        pendingFromAccount = AppSession.getAccountNumber(this);
        pendingToBank = selectedBank();
        pendingToAccount = Ui.text(toAccountInput);
        pendingNote = Ui.text(transferNoteInput);

        if (pendingFromAccount.isEmpty()) {
            resultText.setText("Bạn chưa có tài khoản nguồn. Hãy quay lại dashboard để hệ thống cấp tài khoản.");
            setTransferEnabled(false);
            return;
        }
        if (!isSessionPaymentAccount()) {
            resultText.setText("Chuyển tiền chỉ dùng tài khoản thanh toán.");
            setTransferEnabled(false);
            return;
        }
        if (!INTERNAL_BANK.equals(pendingToBank)) {
            resultText.setText("Hiện backend chỉ hỗ trợ chuyển tiền nội bộ NLU Banking. Liên ngân hàng sẽ làm ở module sau.");
            return;
        }
        if (pendingToAccount.isEmpty()) {
            toAccountInput.setError("Vui lòng nhập tài khoản nhận");
            return;
        }
        if (pendingFromAccount.equals(pendingToAccount)) {
            toAccountInput.setError("Tài khoản nhận phải khác tài khoản nguồn");
            return;
        }

        String amountDigits = digitsOnly(Ui.text(amountInput));
        if (amountDigits.isEmpty()) {
            amountInput.setError("Vui lòng nhập số tiền");
            return;
        }
        pendingAmount = new BigDecimal(amountDigits);
        if (pendingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            amountInput.setError("Số tiền phải lớn hơn 0");
            return;
        }

        confirmSummaryText.setText("Từ tài khoản: " + pendingFromAccount
                + "\nNgân hàng nhận: " + pendingToBank
                + "\nTài khoản nhận: " + pendingToAccount
                + "\nSố tiền: " + CurrencyUtils.formatVnd(pendingAmount)
                + "\nNội dung: " + (pendingNote.isEmpty() ? "Không có" : pendingNote));
        resultText.setText("Kiểm tra thông tin và chọn phương thức xác thực để hoàn tất giao dịch.");
        confirmationPanel.setVisibility(View.VISIBLE);
        renderPinState();
    }

    private void verifyAndSubmit() {
        if (otpEmailOption.isChecked()) {
            resultText.setText("OTP qua email chưa được kích hoạt. Vui lòng chọn xác thực bằng PIN.");
            return;
        }
        if (!AppSession.hasPaymentPin(this)) {
            resultText.setText("Bạn cần thiết lập PIN thanh toán trước khi chuyển tiền.");
            Ui.open(this, PinActivity.class);
            return;
        }
        if (!AppSession.verifyPaymentPin(this, Ui.text(pinInput))) {
            pinInput.setError("PIN không đúng");
            return;
        }
        submitTransfer();
    }

    private void submitTransfer() {
        FundTransferRequest request = new FundTransferRequest(pendingFromAccount, pendingToAccount, pendingAmount);
        resultText.setText("Đang xử lý chuyển tiền...");
        setTransferEnabled(false);
        submitButton.setEnabled(false);

        ApiClient.getApi().transfer(request).enqueue(new Callback<FundTransferResponse>() {
            @Override
            public void onResponse(Call<FundTransferResponse> call, Response<FundTransferResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTransferEnabled(true);
                    renderPinState();
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể chuyển tiền."));
                    return;
                }
                String receipt = buildReceipt(response.body());
                refreshSourceBalance(receipt);
            }

            @Override
            public void onFailure(Call<FundTransferResponse> call, Throwable throwable) {
                setTransferEnabled(true);
                renderPinState();
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private String buildReceipt(FundTransferResponse response) {
        String transactionId = response.transactionId == null || response.transactionId.trim().isEmpty()
                ? "Chưa có"
                : response.transactionId.trim();
        String message = response.message == null || response.message.trim().isEmpty()
                ? "Chuyển tiền đã hoàn tất."
                : response.message.trim();
        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        return "BIÊN LAI CHUYỂN TIỀN"
                + "\nTrạng thái: Thành công"
                + "\nThời gian: " + time
                + "\nMã giao dịch: " + transactionId
                + "\nTừ tài khoản: " + pendingFromAccount
                + "\nNgân hàng nhận: " + pendingToBank
                + "\nĐến tài khoản: " + pendingToAccount
                + "\nSố tiền: " + CurrencyUtils.formatVnd(pendingAmount)
                + "\nNội dung: " + (pendingNote.isEmpty() ? "Không có" : pendingNote)
                + "\nThông báo: " + message;
    }

    private void refreshSourceBalance(String receipt) {
        ApiClient.getApi().getBalance(pendingFromAccount).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                setTransferEnabled(true);
                renderPinState();
                pinInput.setText("");
                if (!response.isSuccessful() || response.body() == null) {
                    if (!response.isSuccessful()) {
                        ApiErrorUtils.httpError(TAG, response, "Không thể tải số dư mới.");
                    }
                    resultText.setText(receipt + "\n\nChưa tải được số dư mới.");
                    renderSourceAccount();
                    return;
                }
                AppSession.saveAccountBalance(TransferActivity.this, response.body());
                renderSourceAccount();
                resultText.setText(receipt + "\n\nSố dư còn lại: " + CurrencyUtils.formatVnd(response.body()));
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                setTransferEnabled(true);
                renderPinState();
                ApiErrorUtils.networkError(TAG, throwable);
                resultText.setText(receipt + "\n\nGiao dịch đã hoàn tất nhưng chưa đồng bộ được số dư.");
                renderSourceAccount();
            }
        });
    }

    private void hideConfirmation() {
        confirmationPanel.setVisibility(View.GONE);
        pinInput.setText("");
    }

    private void setTransferEnabled(boolean enabled) {
        transferButton.setEnabled(enabled);
        bankSpinner.setEnabled(enabled);
        toAccountInput.setEnabled(enabled);
        amountInput.setEnabled(enabled);
        transferNoteInput.setEnabled(enabled);
    }

    private String selectedBank() {
        Object item = bankSpinner.getSelectedItem();
        return item == null ? INTERNAL_BANK : String.valueOf(item);
    }

    private boolean isSessionPaymentAccount() {
        return "PAYMENT_ACCOUNT".equalsIgnoreCase(AppSession.getAccountType(this));
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private String formatMoney(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return "0";
        }
        try {
            String normalized = rawValue.replace(",", "")
                    .replace("đ", "")
                    .trim();
            return moneyFormat.format(new BigDecimal(normalized));
        } catch (NumberFormatException exception) {
            String digits = digitsOnly(rawValue);
            return digits.isEmpty() ? "0" : moneyFormat.format(new BigDecimal(digits));
        }
    }

    private String formatMoney(BigDecimal amount) {
        return amount == null ? "0" : moneyFormat.format(amount);
    }
}
