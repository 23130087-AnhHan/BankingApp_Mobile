package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.FundTransferRequest;
import com.example.bankingmobileapp.model.FundTransferResponse;

import java.math.BigDecimal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferActivity extends Activity {
    private static final String TAG = "TransferActivity";

    private EditText fromAccountInput;
    private EditText toAccountInput;
    private EditText amountInput;
    private EditText transferNoteInput;
    private TextView resultText;
    private Button transferButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        fromAccountInput = findViewById(R.id.fromAccountInput);
        toAccountInput = findViewById(R.id.toAccountInput);
        amountInput = findViewById(R.id.amountInput);
        transferNoteInput = findViewById(R.id.transferNoteInput);
        resultText = findViewById(R.id.resultText);
        transferButton = findViewById(R.id.transferButton);

        if (AppSession.hasAccount(this)) {
            fromAccountInput.setText(AppSession.getAccountNumber(this));
            resultText.setText("Đã chọn tài khoản nguồn mặc định. Bạn chỉ cần nhập người nhận và số tiền.");
        } else {
            resultText.setText("Chưa có tài khoản nguồn. Hãy nhập số tài khoản hoặc mở tài khoản trước.");
        }

        transferButton.setOnClickListener(v -> submitTransfer());
    }

    private void submitTransfer() {
        String fromAccount = Ui.text(fromAccountInput);
        String toAccount = Ui.text(toAccountInput);
        String amountValue = Ui.text(amountInput);
        if (fromAccount.isEmpty()) {
            fromAccountInput.setError("Vui lòng nhập tài khoản nguồn");
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
        if (amountValue.isEmpty()) {
            amountInput.setError("Vui lòng nhập số tiền");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountValue);
        } catch (NumberFormatException ex) {
            amountInput.setError("Số tiền không hợp lệ");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            amountInput.setError("Số tiền phải lớn hơn 0");
            return;
        }

        AppSession.saveAccountNumber(this, fromAccount);
        // Backend request hiện chỉ hỗ trợ fromAccount, toAccount và amount; lời nhắn không được gửi đi.
        FundTransferRequest request = new FundTransferRequest(fromAccount, toAccount, amount);
        resultText.setText("Đang chuyển tiền...");
        transferButton.setEnabled(false);
        ApiClient.getApi().transfer(request).enqueue(new Callback<FundTransferResponse>() {
            @Override
            public void onResponse(Call<FundTransferResponse> call, Response<FundTransferResponse> response) {
                if (!response.isSuccessful()) {
                    transferButton.setEnabled(true);
                    resultText.setText(ApiErrorUtils.httpError(TAG, response, "Không thể chuyển tiền."));
                    return;
                }
                String note = Ui.text(transferNoteInput);
                String receipt = "Chuyển tiền thành công\n" + Ui.formatBody(response.body());
                if (!note.isEmpty()) {
                    receipt += "\nLời nhắn trên thiết bị: " + note;
                }
                refreshSourceBalance(fromAccount, receipt);
            }

            @Override
            public void onFailure(Call<FundTransferResponse> call, Throwable throwable) {
                transferButton.setEnabled(true);
                resultText.setText(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void refreshSourceBalance(String accountNumber, String receipt) {
        ApiClient.getApi().getBalance(accountNumber).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                transferButton.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    if (!response.isSuccessful()) {
                        ApiErrorUtils.httpError(TAG, response, "Không thể tải số dư mới.");
                    }
                    resultText.setText(receipt + "\n\nChưa tải được số dư mới.");
                    return;
                }
                AppSession.saveAccountBalance(TransferActivity.this, response.body());
                resultText.setText(receipt + "\n\nSố dư còn lại: " + response.body() + " ₫");
            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {
                transferButton.setEnabled(true);
                ApiErrorUtils.networkError(TAG, throwable);
                resultText.setText(receipt + "\n\nGiao dịch đã hoàn tất nhưng chưa đồng bộ được số dư.");
            }
        });
    }
}
