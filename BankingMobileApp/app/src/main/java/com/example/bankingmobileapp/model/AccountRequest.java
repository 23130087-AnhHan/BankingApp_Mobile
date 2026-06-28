package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class AccountRequest {
    @SerializedName("accountType")
    public String accountType;
    @SerializedName("availableBalance")
    public BigDecimal availableBalance;
    @SerializedName("userId")
    public Long userId;

    public AccountRequest(String accountType, BigDecimal availableBalance, Long userId) {
        this.accountType = accountType;
        this.availableBalance = availableBalance;
        this.userId = userId;
    }
}
