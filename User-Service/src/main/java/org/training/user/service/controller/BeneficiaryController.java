package org.training.user.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.training.user.service.model.dto.beneficiary.BeneficiaryRequest;
import org.training.user.service.model.dto.beneficiary.BeneficiaryResponse;
import org.training.user.service.model.dto.response.Response;
import org.training.user.service.service.BeneficiaryService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> getBeneficiaries(@PathVariable Long userId) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaries(userId));
    }

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> createBeneficiary(
            @PathVariable Long userId,
            @Valid @RequestBody BeneficiaryRequest request) {
        return new ResponseEntity<>(beneficiaryService.createBeneficiary(userId, request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> deleteBeneficiary(@PathVariable Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(beneficiaryService.deleteBeneficiary(userId, id));
    }
}
