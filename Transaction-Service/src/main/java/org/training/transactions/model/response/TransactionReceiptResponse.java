package org.training.transactions.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionReceiptResponse {

    private Long transactionId;

    private String referenceId;

    private String type;

    private BigDecimal amount;

    private String direction;

    private String status;

    private LocalDateTime time;

    private String description;

    private String fromAccount;

    private String toAccount;

    private String recipientName;

    private String recipientBank;

    private String counterpartyAccount;

    private String counterpartyName;
}
