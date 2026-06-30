package org.training.user.service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "beneficiaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_beneficiary_user_bank_account",
                columnNames = {"user_id", "bank_name", "account_number"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 40)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false, length = 160)
    private String accountHolderName;

    @Column(length = 120)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
