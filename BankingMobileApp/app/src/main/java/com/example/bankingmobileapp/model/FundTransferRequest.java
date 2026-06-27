package com.example.bankingmobileapp.model;

import java.math.BigDecimal;

public class FundTransferRequest {
    public String fromAccount;
    public String toAccount;
    public BigDecimal amount;

    public FundTransferRequest(String fromAccount, String toAccount, BigDecimal amount) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
    }
}
