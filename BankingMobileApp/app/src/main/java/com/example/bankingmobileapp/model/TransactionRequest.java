package com.example.bankingmobileapp.model;

import java.math.BigDecimal;

public class TransactionRequest {
    public String accountId;
    public String transactionType;
    public BigDecimal amount;
    public String description;

    public TransactionRequest(String accountId, String transactionType, BigDecimal amount, String description) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.description = description;
    }
}
