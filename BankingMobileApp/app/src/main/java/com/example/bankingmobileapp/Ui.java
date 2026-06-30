package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.FundTransferResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class Ui {
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
                    resultView.setText(ApiErrorUtils.httpError("Ui", response, action + " chưa thể hoàn tất."));
                    return;
                }
                resultView.setText(action + " hoàn tất\n" + formatBody(response.body()));
            }

            @Override
            public void onFailure(Call<T> call, Throwable throwable) {
                resultView.setText(ApiErrorUtils.networkError("Ui", throwable));
            }
        });
    }

    public static String formatBody(Object body) {
        if (body == null) {
            return "Server không trả nội dung chi tiết.";
        }
        if (body instanceof ApiResponse) {
            ApiResponse response = (ApiResponse) body;
            String message = firstNonEmpty(response.message, response.responseMessage, "Thao tác đã hoàn tất.");
            return "Mã: " + firstNonEmpty(response.responseCode, "--") + "\nThông báo: " + message;
        }
        if (body instanceof FundTransferResponse) {
            FundTransferResponse response = (FundTransferResponse) body;
            return "Mã tham chiếu: " + firstNonEmpty(response.transactionId, "--")
                    + "\nThông báo: " + firstNonEmpty(response.message, "Chuyển tiền đã hoàn tất.");
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
            return "Không tìm thấy tài khoản.";
        }
        if (code == 409) {
            return "Dữ liệu đã tồn tại.";
        }
        if (code >= 500) {
            return "Lỗi hệ thống, vui lòng thử lại.";
        }
        return "Yêu cầu không thành công. Mã lỗi: " + code;
    }

    private static String firstNonEmpty(String first, String fallback) {
        return first == null || first.trim().isEmpty() ? fallback : first;
    }

    private static String firstNonEmpty(String first, String second, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return firstNonEmpty(second, fallback);
    }
}
