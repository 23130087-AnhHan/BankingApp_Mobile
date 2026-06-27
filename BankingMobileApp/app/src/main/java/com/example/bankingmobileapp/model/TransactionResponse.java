package com.example.bankingmobileapp.model;

import java.math.BigDecimal;

public class TransactionResponse {
    public String referenceId;
    public String accountId;
    public String transactionType;
    public BigDecimal amount;
    public String localDateTime;
    public String transactionStatus;
    public String comments;
}
