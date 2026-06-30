package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TransactionRequest {
    @SerializedName("accountNumber")
    public String accountNumber;
    @SerializedName("transactionType")
    public String transactionType;
    @SerializedName("amount")
    public BigDecimal amount;
    @SerializedName("description")
    public String description;

    public TransactionRequest(String accountNumber, String transactionType, BigDecimal amount, String description) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
    }
}
