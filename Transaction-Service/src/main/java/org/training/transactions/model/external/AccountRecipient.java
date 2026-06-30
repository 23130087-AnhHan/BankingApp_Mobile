package org.training.transactions.model.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRecipient {

    private String bankName;

    private String accountNumber;

    private String accountHolderName;

    private String accountType;

    private String accountStatus;
}
