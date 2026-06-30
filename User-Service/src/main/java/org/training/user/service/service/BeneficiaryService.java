package org.training.user.service.service;

import org.training.user.service.model.dto.beneficiary.BeneficiaryRequest;
import org.training.user.service.model.dto.beneficiary.BeneficiaryResponse;
import org.training.user.service.model.dto.response.Response;

import java.util.List;

public interface BeneficiaryService {

    List<BeneficiaryResponse> getBeneficiaries(Long userId);

    BeneficiaryResponse createBeneficiary(Long userId, BeneficiaryRequest request);

    Response deleteBeneficiary(Long userId, Long beneficiaryId);
}
