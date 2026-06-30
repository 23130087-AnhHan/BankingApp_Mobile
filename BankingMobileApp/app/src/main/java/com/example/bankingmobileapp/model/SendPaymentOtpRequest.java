package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class SendPaymentOtpRequest {
    @SerializedName("email")
    public String email;

    public SendPaymentOtpRequest(String email) {
        this.email = email;
    }
}
