package org.training.user.service.model.dto.beneficiary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponse {

    private Long id;

    private Long userId;

    private String bankName;

    private String accountNumber;

    private String accountHolderName;

    private String nickname;

    private LocalDateTime createdAt;
}
