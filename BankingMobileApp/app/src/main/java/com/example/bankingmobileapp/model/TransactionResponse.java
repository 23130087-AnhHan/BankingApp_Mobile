package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TransactionResponse {
    @SerializedName("referenceId")
    public String referenceId;
    @SerializedName("accountId")
    public String accountId;
    @SerializedName("transactionType")
    public String transactionType;
    @SerializedName("amount")
    public BigDecimal amount;
    @SerializedName("localDateTime")
    public String localDateTime;
    @SerializedName("transactionStatus")
    public String transactionStatus;
    @SerializedName("comments")
    public String comments;
}
