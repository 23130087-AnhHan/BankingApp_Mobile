package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    @SerializedName("email")
    public String email;
    @SerializedName("otp")
    public String otp;
    @SerializedName("newPassword")
    public String newPassword;

    public ResetPasswordRequest(String email, String otp, String newPassword) {
        this.email = email;
        this.otp = otp;
        this.newPassword = newPassword;
    }
}
