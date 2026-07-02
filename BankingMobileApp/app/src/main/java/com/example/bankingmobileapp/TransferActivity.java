package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.AccountRecipientResponse;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.BeneficiaryRequest;
import com.example.bankingmobileapp.model.BeneficiaryResponse;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferActivity extends Activity {
    private static final String TAG = "TransferActivity";
    private static final String INTERNAL_BANK = "NLU Banking";
    private static final int REQUEST_SELECT_BENEFICIARY = 301;
    public static final String EXTRA_QR_ACCOUNT_NUMBER = "qr_account_number";
    public static final String EXTRA_QR_ACCOUNT_HOLDER_NAME = "qr_account_holder_name";
    public static final String EXTRA_QR_AMOUNT = "qr_amount";
    public static final String EXTRA_QR_NOTE = "qr_note";

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
    private Button transferButton;
    private Button saveBeneficiaryButton;
    private boolean formattingAmount;
    private boolean recipientVerified;
    private String pendingRecipientName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppSession.hasValidSession(this)) {
            Ui.openAndClear(this, WelcomeActivity.class);
            return;
        }
        setContentView(R.layout.activity_transfer);

        sourceAccountText = findViewById(R.id.sourceAccountText);
        bankSpinner = findViewById(R.id.bankSpinner);
        toAccountInput = findViewById(R.id.toAccountInput);
        recipientNameText = findViewById(R.id.recipientNameText);
        amountInput = findViewById(R.id.amountInput);
        transferNoteInput = findViewById(R.id.transferNoteInput);
        resultText = findViewById(R.id.resultText);
        transferButton = findViewById(R.id.transferButton);
        saveBeneficiaryButton = findViewById(R.id.saveBeneficiaryButton);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.selectBeneficiaryButton).setOnClickListener(v -> openBeneficiaryPicker());
        saveBeneficiaryButton.setOnClickListener(v -> saveCurrentBeneficiary());
        transferButton.setOnClickListener(v -> validateAndOpenConfirm());

        setupBankSpinner();
        setupRecipientLookup();
        setupAmountInput();
        renderSourceAccount();
        applyTransferQrIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderSourceAccount();
    }

    @Override
    protected void onDestroy() {
        lookupHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
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
    }

    private void setupBankSpinner() {
        String[] banks = {INTERNAL_BANK, "Vietcombank", "BIDV", "VietinBank", "Techcombank", "MB Bank"};
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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable value) {
                resetRecipientLookup();
                scheduleRecipientLookup();
            }
        });
    }

    private void setupAmountInput() {
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
                    String formatted = moneyFormat.format(new BigDecimal(digits)) + " đ";
                    amountInput.setText(formatted);
                    amountInput.setSelection(formatted.length() - 2);
                }
                formattingAmount = false;
            }
        });
    }

    private void renderSourceAccount() {
        if (!AppSession.hasAccount(this)) {
            sourceAccountText.setText("Bạn chưa có tài khoản thanh toán. Hãy quay lại trang chủ để hệ thống cấp tài khoản.");
            resultText.setText("Không thể chuyển tiền khi chưa có tài khoản nguồn.");
            transferButton.setEnabled(false);
            return;
        }
        if (!"PAYMENT_ACCOUNT".equalsIgnoreCase(AppSession.getAccountType(this))) {
            sourceAccountText.setText("Tài khoản nguồn hiện tại không phải tài khoản thanh toán.");
            resultText.setText("Chuyển tiền chỉ dùng tài khoản thanh toán.");
            transferButton.setEnabled(false);
            return;
        }
        sourceAccountText.setText("STK nguồn: " + AppSession.getAccountNumber(this)
                + "\nSố dư khả dụng: " + formatMoney(AppSession.getAccountBalance(this)) + " đ");
        transferButton.setEnabled(true);
    }

    private void validateAndOpenConfirm() {
        String fromAccount = AppSession.getAccountNumber(this);
        String toBank = selectedBank();
        String toAccount = Ui.text(toAccountInput);
        String note = Ui.text(transferNoteInput);

        if (fromAccount.isEmpty()) {
            resultText.setText("Bạn chưa có tài khoản nguồn.");
            return;
        }
        if (!INTERNAL_BANK.equals(toBank)) {
            resultText.setText("Hiện backend chỉ hỗ trợ chuyển tiền nội bộ NLU Banking.");
            return;
        }
        if (toAccount.isEmpty()) {
            toAccountInput.setError("Vui lòng nhập tài khoản nhận");
            return;
        }
        if (fromAccount.equals(toAccount)) {
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
        BigDecimal amount = new BigDecimal(amountDigits);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            amountInput.setError("Số tiền phải lớn hơn 0");
            return;
        }
        String balanceStr = AppSession.getAccountBalance(this);
        if (!balanceStr.isEmpty()) {
            try {
                BigDecimal balance = new BigDecimal(balanceStr.replace(",", ""));
                if (amount.compareTo(balance) > 0) {
                    amountInput.setError("Số tiền vượt quá số dư khả dụng (" + CurrencyUtils.formatVnd(balance) + ")");
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        Intent intent = new Intent(this, TransferConfirmActivity.class);
        TransferConfirmActivity.putExtras(intent, fromAccount, toBank, toAccount,
                pendingRecipientName, amount.toPlainString(), note);
        startActivity(intent);
    }

    private void scheduleRecipientLookup() {
        lookupHandler.removeCallbacksAndMessages(null);
        String accountNumber = Ui.text(toAccountInput);
        if (!INTERNAL_BANK.equals(selectedBank())) {
            recipientNameText.setVisibility(View.VISIBLE);
            recipientNameText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            recipientNameText.setText("Hiện chỉ hỗ trợ chuyển tiền nội bộ NLU Banking.");
            return;
        }
        if (accountNumber.length() < 6) {
            recipientNameText.setVisibility(View.GONE);
            return;
        }
        lookupHandler.postDelayed(() -> lookupRecipient(accountNumber), 600);
    }

    private void lookupRecipient(String accountNumber) {
        if (!accountNumber.equals(Ui.text(toAccountInput))) return;
        if (accountNumber.equals(AppSession.getAccountNumber(this))) {
            recipientNameText.setVisibility(View.VISIBLE);
            recipientNameText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            recipientNameText.setText("Tài khoản nhận phải khác tài khoản nguồn.");
            return;
        }
        recipientNameText.setVisibility(View.VISIBLE);
        recipientNameText.setTextColor(getResources().getColor(R.color.text_secondary));
        recipientNameText.setText("Đang kiểm tra người nhận...");
        ApiClient.getApi().getRecipient(accountNumber).enqueue(new Callback<AccountRecipientResponse>() {
            @Override
            public void onResponse(Call<AccountRecipientResponse> call, Response<AccountRecipientResponse> response) {
                if (!accountNumber.equals(Ui.text(toAccountInput))) return;
                if (!response.isSuccessful() || response.body() == null) {
                    recipientVerified = false;
                    pendingRecipientName = "";
                    recipientNameText.setVisibility(View.VISIBLE);
                    recipientNameText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    recipientNameText.setText("Không tìm thấy tài khoản nhận hợp lệ.");
                    return;
                }
                recipientVerified = true;
                pendingRecipientName = safe(response.body().accountHolderName);
                recipientNameText.setVisibility(View.VISIBLE);
                recipientNameText.setTextColor(getResources().getColor(R.color.text_primary));
                recipientNameText.setText("Chủ tài khoản: " + pendingRecipientName);
            }

            @Override
            public void onFailure(Call<AccountRecipientResponse> call, Throwable throwable) {
                recipientVerified = false;
                pendingRecipientName = "";
                recipientNameText.setVisibility(View.VISIBLE);
                recipientNameText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                recipientNameText.setText("Lỗi kết nối mạng.");
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
            resultText.setText("Vui lòng nhập tài khoản nhận hợp lệ trước khi lưu.");
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
                        resultText.setText(response.isSuccessful()
                                ? "Đã lưu người thụ hưởng: " + pendingRecipientName + "."
                                : ApiErrorUtils.httpError(TAG, response, "Không thể lưu người thụ hưởng."));
                    }

                    @Override
                    public void onFailure(Call<BeneficiaryResponse> call, Throwable throwable) {
                        saveBeneficiaryButton.setEnabled(true);
                        resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
                    }
                });
    }

    private void applyTransferQrIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_QR_ACCOUNT_NUMBER)) return;
        String accountNumber = firstNonEmpty(intent.getStringExtra(EXTRA_QR_ACCOUNT_NUMBER), "");
        if (accountNumber.isEmpty() || accountNumber.equals(AppSession.getAccountNumber(this))) return;
        setSelectedBank(TransferQrPayload.BANK_NAME);
        toAccountInput.setText(accountNumber);
        String rawAmount = firstNonEmpty(intent.getStringExtra(EXTRA_QR_AMOUNT), "");
        if (!rawAmount.isEmpty()) amountInput.setText(rawAmount);
        String note = firstNonEmpty(intent.getStringExtra(EXTRA_QR_NOTE), "");
        if (!note.isEmpty()) transferNoteInput.setText(note);
        pendingRecipientName = firstNonEmpty(intent.getStringExtra(EXTRA_QR_ACCOUNT_HOLDER_NAME), "");
        recipientVerified = false;
        recipientNameText.setText(pendingRecipientName.isEmpty()
                ? "Đã quét QR. Hệ thống đang kiểm tra tên chủ tài khoản."
                : "Đã quét QR: " + pendingRecipientName + ". Hệ thống đang kiểm tra lại.");
        scheduleRecipientLookup();
    }

    private void resetRecipientLookup() {
        recipientVerified = false;
        pendingRecipientName = "";
        if (recipientNameText != null) {
            recipientNameText.setVisibility(View.GONE);
        }
    }

    private String selectedBank() {
        Object item = bankSpinner.getSelectedItem();
        return item == null ? INTERNAL_BANK : String.valueOf(item);
    }

    private void setSelectedBank(String bankName) {
        if (bankName == null || bankName.trim().isEmpty()) return;
        for (int i = 0; i < bankSpinner.getCount(); i++) {
            Object item = bankSpinner.getItemAtPosition(i);
            if (item != null && bankName.trim().equalsIgnoreCase(String.valueOf(item))) {
                bankSpinner.setSelection(i);
                return;
            }
        }
    }

    private String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private String formatMoney(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) return "0";
        try {
            String normalized = rawValue.replace(",", "").replace("đ", "").trim();
            return moneyFormat.format(new BigDecimal(normalized));
        } catch (NumberFormatException exception) {
            String digits = digitsOnly(rawValue);
            return digits.isEmpty() ? "0" : moneyFormat.format(new BigDecimal(digits));
        }
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
