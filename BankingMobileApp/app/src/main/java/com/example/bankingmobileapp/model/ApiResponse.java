package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("responseCode")
    public String responseCode;
    @SerializedName("responseMessage")
    public String responseMessage;
    @SerializedName("message")
    public String message;
}
