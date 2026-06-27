package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

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
                    resultView.setText(action + " failed. HTTP " + response.code());
                    return;
                }
                resultView.setText(action + " completed\n" + formatBody(response.body()));
            }

            @Override
            public void onFailure(Call<T> call, Throwable throwable) {
                resultView.setText(action + " failed: " + throwable.getMessage());
            }
        });
    }

    public static String formatBody(Object body) {
        if (body == null) {
            return "No response body";
        }
        if (body instanceof ApiResponse) {
            ApiResponse response = (ApiResponse) body;
            String message = response.message != null ? response.message : response.responseMessage;
            return "Code: " + response.responseCode + "\nMessage: " + message;
        }
        if (body instanceof FundTransferResponse) {
            FundTransferResponse response = (FundTransferResponse) body;
            return "Reference: " + response.transactionId + "\nMessage: " + response.message;
        }
        return String.valueOf(body);
    }
}
