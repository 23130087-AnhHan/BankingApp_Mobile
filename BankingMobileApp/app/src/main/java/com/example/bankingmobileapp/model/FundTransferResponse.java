package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class FundTransferResponse {
    @SerializedName("transactionId")
    public String transactionId;
    @SerializedName("message")
    public String message;
}
