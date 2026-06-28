package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class AccountResponse {
    @SerializedName("accountId")
    public Long accountId;
    @SerializedName("accountNumber")
    public String accountNumber;
    @SerializedName("accountType")
    public String accountType;
    @SerializedName("accountStatus")
    public String accountStatus;
    @SerializedName("availableBalance")
    public BigDecimal availableBalance;
    @SerializedName("userId")
    public Long userId;
}
