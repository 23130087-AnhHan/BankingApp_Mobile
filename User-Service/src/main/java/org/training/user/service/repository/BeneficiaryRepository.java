package org.training.user.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.training.user.service.model.entity.Beneficiary;

import java.util.List;
import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndBankNameIgnoreCaseAndAccountNumber(Long userId, String bankName, String accountNumber);

    Optional<Beneficiary> findByIdAndUserId(Long id, Long userId);
}
