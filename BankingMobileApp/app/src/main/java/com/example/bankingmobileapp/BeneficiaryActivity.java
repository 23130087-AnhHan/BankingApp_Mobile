package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.BeneficiaryRequest;
import com.example.bankingmobileapp.model.BeneficiaryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BeneficiaryActivity extends Activity {
    public static final String EXTRA_SELECT_MODE = "selectMode";
    public static final String EXTRA_BANK_NAME = "bankName";
    public static final String EXTRA_ACCOUNT_NUMBER = "accountNumber";
    public static final String EXTRA_ACCOUNT_HOLDER_NAME = "accountHolderName";

    private static final String TAG = "BeneficiaryActivity";

    private EditText bankNameInput;
    private EditText accountNumberInput;
    private EditText accountHolderNameInput;
    private EditText nicknameInput;
    private TextView resultText;
    private LinearLayout beneficiaryList;
    private Button saveButton;
    private Button refreshButton;
    private boolean selectMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beneficiaries);

        selectMode = getIntent().getBooleanExtra(EXTRA_SELECT_MODE, false);
        bankNameInput = findViewById(R.id.bankNameInput);
        accountNumberInput = findViewById(R.id.beneficiaryAccountInput);
        accountHolderNameInput = findViewById(R.id.accountHolderNameInput);
        nicknameInput = findViewById(R.id.nicknameInput);
        resultText = findViewById(R.id.beneficiaryResultText);
        beneficiaryList = findViewById(R.id.beneficiaryList);
        saveButton = findViewById(R.id.saveBeneficiaryButton);
        refreshButton = findViewById(R.id.refreshBeneficiaryButton);

        bankNameInput.setText("NLU Banking");
        findViewById(R.id.backBeneficiaryButton).setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveBeneficiary());
        refreshButton.setOnClickListener(v -> loadBeneficiaries());
        loadBeneficiaries();
    }

    private void loadBeneficiaries() {
        Long userId = currentUserId();
        if (userId == null) {
            showMessage("Phiên đăng nhập chưa có mã khách hàng. Vui lòng đăng nhập lại.");
            return;
        }
        showMessage("Đang tải danh sách người thụ hưởng...");
        ApiClient.getApi().getBeneficiaries(userId).enqueue(new Callback<List<BeneficiaryResponse>>() {
            @Override
            public void onResponse(Call<List<BeneficiaryResponse>> call, Response<List<BeneficiaryResponse>> response) {
                if (!response.isSuccessful()) {
                    showMessage(ApiErrorUtils.httpError(TAG, response, "Không thể tải người thụ hưởng."));
                    return;
                }
                renderBeneficiaries(response.body());
            }

            @Override
            public void onFailure(Call<List<BeneficiaryResponse>> call, Throwable throwable) {
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void renderBeneficiaries(List<BeneficiaryResponse> beneficiaries) {
        beneficiaryList.removeAllViews();
        if (beneficiaries == null || beneficiaries.isEmpty()) {
            resultText.setText("Bạn chưa lưu người thụ hưởng nào.");
            return;
        }
        resultText.setText(selectMode
                ? "Chọn một người thụ hưởng để điền vào giao dịch."
                : "Danh sách người thụ hưởng đã lưu.");
        for (BeneficiaryResponse beneficiary : beneficiaries) {
            if (beneficiary != null) {
                beneficiaryList.addView(createBeneficiaryView(beneficiary));
            }
        }
    }

    private View createBeneficiaryView(BeneficiaryResponse beneficiary) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.nlu_list_card_bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        TextView title = new TextView(this);
        title.setText(firstNonEmpty(beneficiary.nickname, beneficiary.accountHolderName));
        title.setTextColor(Color.parseColor("#101828"));
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView detail = new TextView(this);
        detail.setText("Chủ tài khoản: " + firstNonEmpty(beneficiary.accountHolderName, "-")
                + "\nSố tài khoản: " + firstNonEmpty(beneficiary.accountNumber, "-")
                + "\nNgân hàng: " + firstNonEmpty(beneficiary.bankName, "NLU Banking"));
        detail.setTextColor(Color.parseColor("#344054"));
        detail.setTextSize(14);
        detail.setPadding(0, dp(8), 0, 0);
        detail.setLineSpacing(dp(2), 1.0f);
        card.addView(detail);

        if (selectMode) {
            TextView action = new TextView(this);
            action.setText("Chọn người thụ hưởng");
            action.setTextColor(Color.parseColor("#075B35"));
            action.setTextSize(14);
            action.setTypeface(Typeface.DEFAULT_BOLD);
            action.setPadding(0, dp(10), 0, 0);
            card.addView(action);
            card.setClickable(true);
            card.setOnClickListener(v -> selectBeneficiary(beneficiary));
        } else {
            Button deleteButton = new Button(this);
            deleteButton.setText("Xóa người thụ hưởng");
            deleteButton.setTextColor(Color.parseColor("#C62828"));
            deleteButton.setAllCaps(false);
            deleteButton.setBackgroundResource(R.drawable.button_warning_bg);
            deleteButton.setOnClickListener(v -> deleteBeneficiary(beneficiary));
            card.addView(deleteButton);
        }
        return card;
    }

    private void saveBeneficiary() {
        Long userId = currentUserId();
        if (userId == null) {
            resultText.setText("Phiên đăng nhập chưa có mã khách hàng. Vui lòng đăng nhập lại.");
            return;
        }
        String bankName = Ui.text(bankNameInput);
        String accountNumber = Ui.text(accountNumberInput);
        String holderName = Ui.text(accountHolderNameInput);
        String nickname = Ui.text(nicknameInput);

        if (bankName.isEmpty()) {
            bankNameInput.setError("Vui lòng nhập ngân hàng");
            return;
        }
        if (accountNumber.isEmpty()) {
            accountNumberInput.setError("Vui lòng nhập số tài khoản");
            return;
        }
        if (holderName.isEmpty()) {
            accountHolderNameInput.setError("Vui lòng nhập tên chủ tài khoản");
            return;
        }

        saveButton.setEnabled(false);
        resultText.setText("Đang lưu người thụ hưởng...");
        ApiClient.getApi()
                .createBeneficiary(userId, new BeneficiaryRequest(bankName, accountNumber, holderName, nickname))
                .enqueue(new Callback<BeneficiaryResponse>() {
                    @Override
                    public void onResponse(Call<BeneficiaryResponse> call, Response<BeneficiaryResponse> response) {
                        saveButton.setEnabled(true);
                        if (!response.isSuccessful()) {
                            resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể lưu người thụ hưởng."));
                            return;
                        }
                        clearForm();
                        resultText.setText("Đã lưu người thụ hưởng.");
                        loadBeneficiaries();
                    }

                    @Override
                    public void onFailure(Call<BeneficiaryResponse> call, Throwable throwable) {
                        saveButton.setEnabled(true);
                        resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
                    }
                });
    }

    private void deleteBeneficiary(BeneficiaryResponse beneficiary) {
        Long userId = currentUserId();
        if (userId == null || beneficiary.id == null) {
            resultText.setText("Không thể xác định người thụ hưởng cần xóa.");
            return;
        }
        resultText.setText("Đang xóa người thụ hưởng...");
        ApiClient.getApi().deleteBeneficiary(userId, beneficiary.id).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (!response.isSuccessful()) {
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể xóa người thụ hưởng."));
                    return;
                }
                resultText.setText("Đã xóa người thụ hưởng.");
                loadBeneficiaries();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void selectBeneficiary(BeneficiaryResponse beneficiary) {
        Intent result = new Intent();
        result.putExtra(EXTRA_BANK_NAME, firstNonEmpty(beneficiary.bankName, "NLU Banking"));
        result.putExtra(EXTRA_ACCOUNT_NUMBER, firstNonEmpty(beneficiary.accountNumber, ""));
        result.putExtra(EXTRA_ACCOUNT_HOLDER_NAME, firstNonEmpty(beneficiary.accountHolderName, ""));
        setResult(RESULT_OK, result);
        finish();
    }

    private void clearForm() {
        bankNameInput.setText("NLU Banking");
        accountNumberInput.setText("");
        accountHolderNameInput.setText("");
        nicknameInput.setText("");
    }

    private void showMessage(String message) {
        resultText.setText(message);
        beneficiaryList.removeAllViews();
    }

    private Long currentUserId() {
        try {
            return Long.parseLong(AppSession.getUserId(this));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String firstNonEmpty(String first, String fallback) {
        return first == null || first.trim().isEmpty() ? fallback : first.trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
