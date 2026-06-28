package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class FundTransferRequest {
    @SerializedName("fromAccount")
    public String fromAccount;
    @SerializedName("toAccount")
    public String toAccount;
    @SerializedName("amount")
    public BigDecimal amount;

    public FundTransferRequest(String fromAccount, String toAccount, BigDecimal amount) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
    }
}
