package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class ResendEmailOtpRequest {
    @SerializedName("email")
    public String email;

    public ResendEmailOtpRequest(String email) {
        this.email = email;
    }
}
