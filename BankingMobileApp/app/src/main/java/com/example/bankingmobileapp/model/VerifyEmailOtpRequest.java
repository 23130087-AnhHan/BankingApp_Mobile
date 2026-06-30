package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class VerifyEmailOtpRequest {
    @SerializedName("email")
    public String email;

    @SerializedName("otp")
    public String otp;

    public VerifyEmailOtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }
}
