package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TransactionRequest {
    @SerializedName("accountId")
    public String accountId;
    @SerializedName("transactionType")
    public String transactionType;
    @SerializedName("amount")
    public BigDecimal amount;
    @SerializedName("description")
    public String description;

    public TransactionRequest(String accountId, String transactionType, BigDecimal amount, String description) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
    }
}
