package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class TransactionResponse {
    @SerializedName("transactionId")
    public Long transactionId;
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
    @SerializedName("direction")
    public String direction;
    @SerializedName("signedAmount")
    public BigDecimal signedAmount;
    @SerializedName("displayTitle")
    public String displayTitle;
    @SerializedName("displayMessage")
    public String displayMessage;
    @SerializedName("counterpartyAccount")
    public String counterpartyAccount;
    @SerializedName("counterpartyName")
    public String counterpartyName;
    @SerializedName("bankName")
    public String bankName;
    @SerializedName("type")
    public String type;
    @SerializedName("status")
    public String status;
    @SerializedName("time")
    public String time;
    @SerializedName("description")
    public String description;
    @SerializedName("fromAccount")
    public String fromAccount;
    @SerializedName("toAccount")
    public String toAccount;
    @SerializedName("recipientName")
    public String recipientName;
    @SerializedName("recipientBank")
    public String recipientBank;
}
