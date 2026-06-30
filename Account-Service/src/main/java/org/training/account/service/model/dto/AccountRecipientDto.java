package org.training.account.service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRecipientDto {
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String accountType;
    private String accountStatus;
}
