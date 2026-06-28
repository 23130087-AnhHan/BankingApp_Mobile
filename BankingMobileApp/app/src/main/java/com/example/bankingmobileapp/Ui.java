package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.FundTransferResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class Ui {
    private static final String TAG = "BankingUi";

    private Ui() {
    }

    public static String text(EditText input) {
        return input.getText().toString().trim();
    }

    public static void open(Activity activity, Class<?> screen) {
        activity.startActivity(new Intent(activity, screen));
    }

    public static void openAndClear(Activity activity, Class<?> screen) {
        Intent intent = new Intent(activity, screen);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static <T> void runCall(String action, TextView resultView, Call<T> call) {
        resultView.setText(action + " đang xử lý...");
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, action + " failed. HTTP " + response.code());
                    resultView.setText(action + " thất bại.\n" + messageForHttpCode(response.code()));
                    return;
                }
                resultView.setText(action + " hoàn tất\n" + formatBody(response.body()));
            }

            @Override
            public void onFailure(Call<T> call, Throwable throwable) {
                Log.e(TAG, action + " network failure", throwable);
                resultView.setText(action + " thất bại.\nKhông kết nối được server.");
            }
        });
    }

    public static String formatBody(Object body) {
        if (body == null) {
            return "Không có dữ liệu phản hồi";
        }
        if (body instanceof ApiResponse) {
            ApiResponse response = (ApiResponse) body;
            String message = response.message != null ? response.message : response.responseMessage;
            return "Mã phản hồi: " + response.responseCode + "\nThông báo: " + message;
        }
        if (body instanceof FundTransferResponse) {
            FundTransferResponse response = (FundTransferResponse) body;
            return "Mã tham chiếu: " + response.transactionId + "\nThông báo: " + response.message;
        }
        return String.valueOf(body);
    }

    public static String messageForHttpCode(int code) {
        if (code == 400) {
            return "Dữ liệu không hợp lệ.";
        }
        if (code == 401 || code == 403) {
            return "Thông tin đăng nhập không hợp lệ hoặc chưa được cấp quyền.";
        }
        if (code == 404) {
            return "Không tìm thấy dữ liệu.";
        }
        if (code == 409) {
            return "Dữ liệu đã tồn tại hoặc xung đột.";
        }
        if (code >= 500) {
            return "Lỗi hệ thống, vui lòng thử lại.";
        }
        return "Yêu cầu không thành công. Mã lỗi: " + code;
    }
}
