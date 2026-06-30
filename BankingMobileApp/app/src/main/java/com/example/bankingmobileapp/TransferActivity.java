package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.AccountRecipientResponse;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.BeneficiaryRequest;
import com.example.bankingmobileapp.model.BeneficiaryResponse;
import com.example.bankingmobileapp.model.FundTransferRequest;
import com.example.bankingmobileapp.model.FundTransferResponse;
import com.example.bankingmobileapp.model.SendPaymentOtpRequest;
import com.example.bankingmobileapp.model.VerifyPaymentOtpRequest;

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
    private static final int REQUEST_SELECT_BENEFICIARY = 301;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0",
            DecimalFormatSymbols.getInstance(Locale.US));
    private final Handler lookupHandler = new Handler(Looper.getMainLooper());

    private TextView sourceAccountText;
    private Spinner bankSpinner;
    private EditText toAccountInput;
    private TextView recipientNameText;
    private EditText amountInput;
    private EditText transferNoteInput;
    private TextView resultText;
    private TextView confirmSummaryText;
    private View confirmationPanel;
    private RadioGroup authMethodGroup;
    private RadioButton otpEmailOption;
    private RadioButton pinOption;
    private EditText pinInput;
    private EditText otpInput;
    private Button sendOtpButton;
    private Button selectBeneficiaryButton;
    private Button saveBeneficiaryButton;
    private Button transferButton;
    private Button submitButton;
    private Button editButton;
    private Button managePinButton;

    private String pendingFromAccount;
    private String pendingToBank;
    private String pendingToAccount;
    private String pendingRecipientName;
    private BigDecimal pendingAmount;
    private String pendingNote;
    private boolean formattingAmount;
    private boolean recipientVerified;
    private boolean paymentOtpSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        sourceAccountText = findViewById(R.id.sourceAccountText);
        bankSpinner = findViewById(R.id.bankSpinner);
        toAccountInput = findViewById(R.id.toAccountInput);
        recipientNameText = findViewById(R.id.recipientNameText);
        amountInput = findViewById(R.id.amountInput);
        transferNoteInput = findViewById(R.id.transferNoteInput);
        resultText = findViewById(R.id.resultText);
        confirmSummaryText = findViewById(R.id.confirmSummaryText);
        confirmationPanel = findViewById(R.id.confirmationPanel);
        authMethodGroup = findViewById(R.id.authMethodGroup);
        otpEmailOption = findViewById(R.id.otpEmailOption);
        pinOption = findViewById(R.id.pinOption);
        pinInput = findViewById(R.id.pinInput);
        otpInput = findViewById(R.id.otpInput);
        sendOtpButton = findViewById(R.id.sendOtpButton);
        selectBeneficiaryButton = findViewById(R.id.selectBeneficiaryButton);
        saveBeneficiaryButton = findViewById(R.id.saveBeneficiaryButton);
        transferButton = findViewById(R.id.transferButton);
        submitButton = findViewById(R.id.submitButton);
        editButton = findViewById(R.id.editButton);
        managePinButton = findViewById(R.id.managePinButton);

        setupBankSpinner();
        setupRecipientLookup();
        setupAmountInput();
        setupSecureInputs();
        setupAuthMethodSwitching();

        transferButton.setOnClickListener(v -> validateAndShowConfirmation());
        selectBeneficiaryButton.setOnClickListener(v -> openBeneficiaryPicker());
        saveBeneficiaryButton.setOnClickListener(v -> saveCurrentBeneficiary());
        sendOtpButton.setOnClickListener(v -> sendPaymentOtp());
        submitButton.setOnClickListener(v -> verifyAndSubmit());
        editButton.setOnClickListener(v -> hideConfirmation());
        managePinButton.setOnClickListener(v -> Ui.open(this, PinActivity.class));

        renderSourceAccount();
        hideConfirmation();
        renderAuthState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SELECT_BENEFICIARY || resultCode != RESULT_OK || data == null) {
            return;
        }
        String bankName = data.getStringExtra(BeneficiaryActivity.EXTRA_BANK_NAME);
        String accountNumber = data.getStringExtra(BeneficiaryActivity.EXTRA_ACCOUNT_NUMBER);
        String holderName = data.getStringExtra(BeneficiaryActivity.EXTRA_ACCOUNT_HOLDER_NAME);

        setSelectedBank(firstNonEmpty(bankName, INTERNAL_BANK));
        toAccountInput.setText(firstNonEmpty(accountNumber, ""));
        pendingRecipientName = firstNonEmpty(holderName, "");
        recipientVerified = !pendingRecipientName.isEmpty();
        recipientNameText.setText(recipientVerified
                ? "Chủ tài khoản: " + pendingRecipientName
                : "Đã chọn người thụ hưởng. Hệ thống đang kiểm tra lại tên chủ tài khoản.");
        hideConfirmation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderSourceAccount();
        renderAuthState();
    }

    @Override
    protected void onDestroy() {
        lookupHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
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
        bankSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                resetRecipientLookup();
                scheduleRecipientLookup();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupRecipientLookup() {
        toAccountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable value) {
                resetRecipientLookup();
                scheduleRecipientLookup();
            }
        });
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
                amountInput.setText(digits.isEmpty() ? "" : moneyFormat.format(new BigDecimal(digits)));
                amountInput.setSelection(amountInput.getText().length());
                formattingAmount = false;
                hideConfirmation();
            }
        });
    }

    private void setupSecureInputs() {
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        otpInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        otpInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
    }

    private void setupAuthMethodSwitching() {
        authMethodGroup.setOnCheckedChangeListener((group, checkedId) -> renderAuthState());
    }

    private void scheduleRecipientLookup() {
        lookupHandler.removeCallbacksAndMessages(null);
        String accountNumber = Ui.text(toAccountInput);
        if (!INTERNAL_BANK.equals(selectedBank())) {
            recipientNameText.setText("Hiện chỉ hỗ trợ kiểm tra người nhận nội bộ NLU Banking.");
            return;
        }
        if (accountNumber.length() < 6) {
            recipientNameText.setText("Nhập số tài khoản để kiểm tra tên chủ tài khoản.");
            return;
        }
        lookupHandler.postDelayed(() -> lookupRecipient(accountNumber), 600);
    }

    private void lookupRecipient(String accountNumber) {
        if (!accountNumber.equals(Ui.text(toAccountInput))) {
            return;
        }
        if (accountNumber.equals(AppSession.getAccountNumber(this))) {
            recipientNameText.setText("Tài khoản nhận phải khác tài khoản nguồn.");
            return;
        }

        recipientNameText.setText("Đang kiểm tra người nhận...");
        ApiClient.getApi().getRecipient(accountNumber).enqueue(new Callback<AccountRecipientResponse>() {
            @Override
            public void onResponse(Call<AccountRecipientResponse> call, Response<AccountRecipientResponse> response) {
                if (!accountNumber.equals(Ui.text(toAccountInput))) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    recipientVerified = false;
                    pendingRecipientName = "";
                    recipientNameText.setText(ApiErrorUtils.httpError(TAG, response,
                            "Không tìm thấy tài khoản nhận hợp lệ."));
                    return;
                }
                AccountRecipientResponse recipient = response.body();
                recipientVerified = true;
                pendingRecipientName = safe(recipient.accountHolderName);
                recipientNameText.setText("Chủ tài khoản: " + pendingRecipientName);
            }

            @Override
            public void onFailure(Call<AccountRecipientResponse> call, Throwable throwable) {
                recipientVerified = false;
                pendingRecipientName = "";
                recipientNameText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void openBeneficiaryPicker() {
        Intent intent = new Intent(this, BeneficiaryActivity.class);
        intent.putExtra(BeneficiaryActivity.EXTRA_SELECT_MODE, true);
        startActivityForResult(intent, REQUEST_SELECT_BENEFICIARY);
    }

    private void saveCurrentBeneficiary() {
        Long userId = currentUserId();
        if (userId == null) {
            resultText.setText("Phiên đăng nhập chưa có mã khách hàng. Vui lòng đăng nhập lại.");
            return;
        }
        String bankName = selectedBank();
        String accountNumber = Ui.text(toAccountInput);
        if (!INTERNAL_BANK.equals(bankName)) {
            resultText.setText("Hiện chỉ lưu người thụ hưởng nội bộ NLU Banking.");
            return;
        }
        if (accountNumber.isEmpty()) {
            toAccountInput.setError("Vui lòng nhập số tài khoản nhận");
            return;
        }
        if (!recipientVerified || pendingRecipientName.isEmpty()) {
            resultText.setText("Vui lòng nhập tài khoản nhận hợp lệ và chờ hệ thống hiển thị tên chủ tài khoản trước khi lưu.");
            return;
        }

        saveBeneficiaryButton.setEnabled(false);
        resultText.setText("Đang lưu người thụ hưởng...");
        ApiClient.getApi()
                .createBeneficiary(userId,
                        new BeneficiaryRequest(bankName, accountNumber, pendingRecipientName, pendingRecipientName))
                .enqueue(new Callback<BeneficiaryResponse>() {
                    @Override
                    public void onResponse(Call<BeneficiaryResponse> call, Response<BeneficiaryResponse> response) {
                        saveBeneficiaryButton.setEnabled(true);
                        if (!response.isSuccessful()) {
                            resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể lưu người thụ hưởng."));
                            return;
                        }
                        resultText.setText("Đã lưu người thụ hưởng: " + pendingRecipientName + ".");
                    }

                    @Override
                    public void onFailure(Call<BeneficiaryResponse> call, Throwable throwable) {
                        saveBeneficiaryButton.setEnabled(true);
                        resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
                    }
                });
    }

    private void resetRecipientLookup() {
        recipientVerified = false;
        pendingRecipientName = "";
        paymentOtpSent = false;
        hideConfirmation();
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
                + "\nSố dư khả dụng: " + formatMoney(balance) + " đ");
        setTransferEnabled(true);
    }

    private void renderAuthState() {
        boolean usingPin = pinOption.isChecked();
        boolean hasPin = AppSession.hasPaymentPin(this);

        pinInput.setVisibility(usingPin ? View.VISIBLE : View.GONE);
        managePinButton.setVisibility(usingPin && !hasPin ? View.VISIBLE : View.GONE);
        pinInput.setEnabled(usingPin && hasPin);
        if (usingPin) {
            pinInput.setHint(hasPin ? "Nhập PIN 6 số" : "Bạn cần thiết lập PIN trước");
        }

        otpInput.setVisibility(usingPin ? View.GONE : View.VISIBLE);
        sendOtpButton.setVisibility(usingPin ? View.GONE : View.VISIBLE);
        otpInput.setEnabled(!usingPin && paymentOtpSent);
        if (!usingPin) {
            otpInput.setHint(paymentOtpSent ? "Nhập OTP 6 số" : "Bấm gửi OTP trước");
        }
        submitButton.setEnabled(usingPin ? hasPin : paymentOtpSent);
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
            resultText.setText("Chuyển tiền chỉ dùng tài khoản thanh toán chuẩn.");
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
        if (!recipientVerified || pendingRecipientName.isEmpty()) {
            resultText.setText("Vui lòng nhập tài khoản nhận hợp lệ và chờ hệ thống hiển thị tên chủ tài khoản.");
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
                + "\nChủ tài khoản: " + pendingRecipientName
                + "\nSố tiền: " + CurrencyUtils.formatVnd(pendingAmount)
                + "\nNội dung: " + (pendingNote.isEmpty() ? "Không có" : pendingNote));
        resultText.setText("Kiểm tra thông tin và chọn phương thức xác thực để hoàn tất giao dịch.");
        confirmationPanel.setVisibility(View.VISIBLE);
        renderAuthState();
    }

    private void sendPaymentOtp() {
        String email = AppSession.getUserEmail(this);
        if (email.isEmpty()) {
            resultText.setText("Phiên đăng nhập chưa có email để gửi OTP.");
            return;
        }
        sendOtpButton.setEnabled(false);
        resultText.setText("Đang gửi OTP thanh toán tới email...");
        ApiClient.getApi().sendPaymentOtp(new SendPaymentOtpRequest(email)).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                sendOtpButton.setEnabled(true);
                if (!response.isSuccessful()) {
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể gửi OTP thanh toán."));
                    return;
                }
                paymentOtpSent = true;
                otpInput.setText("");
                resultText.setText("Đã gửi OTP thanh toán tới email " + email + ".");
                renderAuthState();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                sendOtpButton.setEnabled(true);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void verifyAndSubmit() {
        if (pinOption.isChecked()) {
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
            return;
        }
        verifyPaymentOtpAndSubmit();
    }

    private void verifyPaymentOtpAndSubmit() {
        String otp = Ui.text(otpInput);
        if (!paymentOtpSent) {
            resultText.setText("Vui lòng gửi OTP thanh toán trước.");
            return;
        }
        if (!otp.matches("\\d{6}")) {
            otpInput.setError("OTP phải gồm 6 chữ số");
            return;
        }
        submitButton.setEnabled(false);
        resultText.setText("Đang xác thực OTP thanh toán...");
        ApiClient.getApi().verifyPaymentOtp(new VerifyPaymentOtpRequest(AppSession.getUserEmail(this), otp))
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (!response.isSuccessful()) {
                            submitButton.setEnabled(true);
                            resultText.setText(ApiErrorUtils.httpError(TAG, response,
                                    "Không thể xác thực OTP thanh toán."));
                            return;
                        }
                        submitTransfer();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                        submitButton.setEnabled(true);
                        resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
                    }
                });
    }

    private void submitTransfer() {
        FundTransferRequest request = new FundTransferRequest(pendingFromAccount, pendingToAccount, pendingAmount, pendingNote);
        resultText.setText("Đang xử lý chuyển tiền...");
        setTransferEnabled(false);
        submitButton.setEnabled(false);

        ApiClient.getApi().transfer(request).enqueue(new Callback<FundTransferResponse>() {
            @Override
            public void onResponse(Call<FundTransferResponse> call, Response<FundTransferResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setTransferEnabled(true);
                    renderAuthState();
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể chuyển tiền."));
                    return;
                }
                String receipt = buildReceipt(response.body());
                refreshSourceBalance(receipt);
            }

            @Override
            public void onFailure(Call<FundTransferResponse> call, Throwable throwable) {
                setTransferEnabled(true);
                renderAuthState();
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
                + "\nChủ tài khoản: " + pendingRecipientName
                + "\nSố tiền: " + CurrencyUtils.formatVnd(pendingAmount)
                + "\nNội dung: " + (pendingNote.isEmpty() ? "Không có" : pendingNote)
                + "\nThông báo: " + message;
    }

    private void refreshSourceBalance(String receipt) {
        ApiClient.getApi().getBalance(pendingFromAccount).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                setTransferEnabled(true);
                paymentOtpSent = false;
                pinInput.setText("");
                otpInput.setText("");
                renderAuthState();
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
                resultText.setText(receipt + "\n\nSố dư còn lại: " + formatMoney(response.body()) + " đ");
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                setTransferEnabled(true);
                renderAuthState();
                ApiErrorUtils.networkError(TAG, throwable);
                resultText.setText(receipt + "\n\nGiao dịch đã hoàn tất nhưng chưa đồng bộ được số dư.");
                renderSourceAccount();
            }
        });
    }

    private void hideConfirmation() {
        confirmationPanel.setVisibility(View.GONE);
        pinInput.setText("");
        otpInput.setText("");
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

    private void setSelectedBank(String bankName) {
        if (bankName == null || bankName.trim().isEmpty()) {
            return;
        }
        for (int index = 0; index < bankSpinner.getCount(); index++) {
            Object item = bankSpinner.getItemAtPosition(index);
            if (item != null && bankName.trim().equalsIgnoreCase(String.valueOf(item))) {
                bankSpinner.setSelection(index);
                return;
            }
        }
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

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Khach hang NLU Banking" : value.trim();
    }

    private String firstNonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private Long currentUserId() {
        try {
            return Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
