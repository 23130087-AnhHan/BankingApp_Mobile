package org.training.user.service.service.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.training.user.service.exception.ResourceConflictException;
import org.training.user.service.exception.ResourceNotFound;
import org.training.user.service.model.dto.beneficiary.BeneficiaryRequest;
import org.training.user.service.model.dto.beneficiary.BeneficiaryResponse;
import org.training.user.service.model.dto.response.Response;
import org.training.user.service.model.entity.Beneficiary;
import org.training.user.service.repository.BeneficiaryRepository;
import org.training.user.service.repository.UserRepository;
import org.training.user.service.service.BeneficiaryService;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BeneficiaryServiceImpl implements BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    @Value("${spring.application.success}")
    private String responseCodeSuccess;

    @Override
    public List<BeneficiaryResponse> getBeneficiaries(Long userId) {
        ensureUserExists(userId);
        return beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BeneficiaryResponse createBeneficiary(Long userId, BeneficiaryRequest request) {
        ensureUserExists(userId);

        String bankName = normalizeRequired(request.getBankName());
        String accountNumber = normalizeRequired(request.getAccountNumber());
        String accountHolderName = normalizeRequired(request.getAccountHolderName());
        String nickname = normalizeOptional(request.getNickname());

        if (beneficiaryRepository.existsByUserIdAndBankNameIgnoreCaseAndAccountNumber(userId, bankName, accountNumber)) {
            throw new ResourceConflictException("Người thụ hưởng này đã tồn tại trong danh sách");
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .userId(userId)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolderName(accountHolderName)
                .nickname(nickname)
                .build();

        return toResponse(beneficiaryRepository.save(beneficiary));
    }

    @Override
    public Response deleteBeneficiary(Long userId, Long beneficiaryId) {
        ensureUserExists(userId);
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
                .orElseThrow(() -> new ResourceNotFound("Không tìm thấy người thụ hưởng"));
        beneficiaryRepository.delete(beneficiary);
        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Đã xóa người thụ hưởng")
                .build();
    }

    private void ensureUserExists(Long userId) {
        if (userId == null || userRepository.findById(userId).isEmpty()) {
            throw new ResourceNotFound("Không tìm thấy khách hàng");
        }
    }

    private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .userId(beneficiary.getUserId())
                .bankName(beneficiary.getBankName())
                .accountNumber(beneficiary.getAccountNumber())
                .accountHolderName(beneficiary.getAccountHolderName())
                .nickname(beneficiary.getNickname())
                .createdAt(beneficiary.getCreatedAt())
                .build();
    }

    private String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
