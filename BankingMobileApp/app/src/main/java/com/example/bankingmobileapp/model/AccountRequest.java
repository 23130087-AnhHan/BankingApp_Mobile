package com.example.bankingmobileapp.model;

import java.math.BigDecimal;

public class AccountRequest {
    public String accountType;
    public BigDecimal availableBalance;
    public Long userId;

    public AccountRequest(String accountType, BigDecimal availableBalance, Long userId) {
        this.accountType = accountType;
        this.availableBalance = availableBalance;
        this.userId = userId;
    }
}
