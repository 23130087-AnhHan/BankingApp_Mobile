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

    public static <T> void runCall(String action, TextView resultView, Call<T> call) {
        resultView.setText(action + " is processing...");
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (!response.isSuccessful()) {
                    resultView.setText(ApiErrorUtils.httpError("Ui", response, action + " chưa thể hoàn tất."));
                    return;
                }
                resultView.setText(action + " completed\n" + formatBody(response.body()));
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
