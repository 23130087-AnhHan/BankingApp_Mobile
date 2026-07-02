package org.training.fundtransfer.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.training.fundtransfer.exception.AccountUpdateException;
import org.training.fundtransfer.exception.GlobalErrorCode;
import org.training.fundtransfer.exception.InsufficientBalance;
import org.training.fundtransfer.exception.ResourceNotFound;
import org.training.fundtransfer.external.AccountService;
import org.training.fundtransfer.external.TransactionService;
import org.training.fundtransfer.external.UserService;
import org.training.fundtransfer.model.mapper.FundTransferMapper;
import org.training.fundtransfer.model.TransactionStatus;
import org.training.fundtransfer.model.TransferType;
import org.training.fundtransfer.model.dto.Account;
import org.training.fundtransfer.model.dto.FundTransferDto;
import org.training.fundtransfer.model.dto.NotificationRequest;
import org.training.fundtransfer.model.dto.Transaction;
import org.training.fundtransfer.model.dto.request.FundTransferRequest;
import org.training.fundtransfer.model.dto.response.FundTransferResponse;
import org.training.fundtransfer.model.entity.FundTransfer;
import org.training.fundtransfer.repository.FundTransferRepository;
import org.training.fundtransfer.service.FundTransferService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundTransferServiceImpl implements FundTransferService {

    private final AccountService accountService;
    private final FundTransferRepository fundTransferRepository;
    private final TransactionService transactionService;
    private final UserService userService;

    @Value("${spring.application.ok}")
    private String ok;

    private final FundTransferMapper fundTransferMapper = new FundTransferMapper();

    /**
     * Transfers funds from one account to another.
     *
     * @param fundTransferRequest The request object containing the details of the fund transfer.
     * @return The response object indicating the status of the fund transfer.
     * @throws ResourceNotFound If the requested account is not found on the server.
     * @throws AccountUpdateException If the account status is pending or inactive.
     * @throws InsufficientBalance If the required amount to transfer is not available.
     */
    @Override
    public FundTransferResponse fundTransfer(FundTransferRequest fundTransferRequest) {

        validateRequest(fundTransferRequest);

        Account fromAccount;
        ResponseEntity<Account> response = accountService.readByAccountNumber(fundTransferRequest.getFromAccount());
        if(Objects.isNull(response.getBody())){
            log.error("requested account "+fundTransferRequest.getFromAccount()+" is not found on the server");
            throw new ResourceNotFound("requested account not found on the server", GlobalErrorCode.NOT_FOUND);
        }
        fromAccount = response.getBody();

        Account toAccount;
        response = accountService.readByAccountNumber(fundTransferRequest.getToAccount());
        if(Objects.isNull(response.getBody())) {
            log.error("requested account "+fundTransferRequest.getToAccount()+" is not found on the server");
            throw new ResourceNotFound("requested account not found on the server", GlobalErrorCode.NOT_FOUND);
        }
        toAccount = response.getBody();

        validateAccounts(fromAccount, toAccount, fundTransferRequest.getAmount());

        // Simple security check: if OTP is provided, it should be 6 digits (Mock verification)
        if (fundTransferRequest.getOtp() != null && fundTransferRequest.getOtp().length() != 6) {
             throw new AccountUpdateException("Invalid OTP provided", GlobalErrorCode.BAD_REQUEST);
        }

        String transactionId = internalTransfer(fromAccount, toAccount, fundTransferRequest.getAmount(), fundTransferRequest.getDescription());
        FundTransfer fundTransfer = FundTransfer.builder()
                .transferType(TransferType.INTERNAL)
                .amount(fundTransferRequest.getAmount())
                .fromAccount(fromAccount.getAccountNumber())
                .transactionReference(transactionId)
                .status(TransactionStatus.SUCCESS)
                .toAccount(toAccount.getAccountNumber()).build();

        fundTransferRepository.save(fundTransfer);
        createTransferNotifications(fromAccount, toAccount, fundTransferRequest.getAmount(), transactionId);
        return FundTransferResponse.builder()
                .transactionId(transactionId)
                .message("Fund transfer was successful").build();
    }

    private void validateRequest(FundTransferRequest request) {
        if (Objects.isNull(request)
                || isBlank(request.getFromAccount())
                || isBlank(request.getToAccount())
                || Objects.isNull(request.getAmount())) {
            throw new AccountUpdateException("Transfer request is missing required information", GlobalErrorCode.BAD_REQUEST);
        }
        if (request.getFromAccount().trim().equals(request.getToAccount().trim())) {
            throw new AccountUpdateException("Source and destination accounts must be different", GlobalErrorCode.BAD_REQUEST);
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AccountUpdateException("Transfer amount must be greater than zero", GlobalErrorCode.BAD_REQUEST);
        }
    }

    private void validateAccounts(Account fromAccount, Account toAccount, BigDecimal amount) {
        if (!isActive(fromAccount)) {
            log.error("source account is not active");
            throw new AccountUpdateException("Source account is not active. Please activate the account before transferring money.", GlobalErrorCode.NOT_ACCEPTABLE);
        }
        if (!isActive(toAccount)) {
            log.error("destination account is not active");
            throw new AccountUpdateException("Destination account is not active. Please choose an active receiving account.", GlobalErrorCode.NOT_ACCEPTABLE);
        }
        if (!isPaymentAccount(fromAccount)) {
            log.error("source account is not a payment account");
            throw new AccountUpdateException("Source account must be a payment account.", GlobalErrorCode.NOT_ACCEPTABLE);
        }
        if (!isPaymentAccount(toAccount)) {
            log.error("destination account is not a payment account");
            throw new AccountUpdateException("Destination account must be a payment account.", GlobalErrorCode.NOT_ACCEPTABLE);
        }
        if (Objects.isNull(fromAccount.getAvailableBalance())
                || fromAccount.getAvailableBalance().compareTo(amount) < 0) {
            log.error("required amount to transfer is not available");
            throw new InsufficientBalance("Insufficient balance to complete this transfer.", GlobalErrorCode.NOT_ACCEPTABLE);
        }
    }

    private boolean isActive(Account account) {
        return !Objects.isNull(account)
                && !Objects.isNull(account.getAccountStatus())
                && "ACTIVE".equalsIgnoreCase(account.getAccountStatus().trim());
    }

    private boolean isPaymentAccount(Account account) {
        if (Objects.isNull(account) || Objects.isNull(account.getAccountType())) {
            return false;
        }
        String accountType = account.getAccountType().trim();
        return "PAYMENT_ACCOUNT".equalsIgnoreCase(accountType);
    }

    private void createTransferNotifications(
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            String transactionId) {
        createNotification(NotificationRequest.builder()
                .userId(fromAccount.getUserId())
                .title("Chuyển tiền thành công")
                .message("Bạn đã chuyển " + formatAmount(amount)
                        + " đ đến tài khoản " + toAccount.getAccountNumber() + ".")
                .type("TRANSFER_OUT")
                .referenceId(transactionId)
                .build());

        createNotification(NotificationRequest.builder()
                .userId(toAccount.getUserId())
                .title("Nhận tiền thành công")
                .message("Bạn đã nhận " + formatAmount(amount)
                        + " đ từ tài khoản " + fromAccount.getAccountNumber() + ".")
                .type("TRANSFER_IN")
                .referenceId(transactionId)
                .build());
    }

    private void createNotification(NotificationRequest request) {
        if (Objects.isNull(request.getUserId())) {
            log.warn("Skipping notification because userId is missing for reference {}", request.getReferenceId());
            return;
        }
        try {
            userService.createNotification(request);
        } catch (Exception exception) {
            log.warn("Transfer {} completed but notification for user {} could not be created",
                    request.getReferenceId(), request.getUserId(), exception);
        }
    }

    private String formatAmount(BigDecimal amount) {
        return Objects.isNull(amount) ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.trim().isEmpty();
    }

    /**
     * Transfers funds from one account to another within the system.
     *
     * @param fromAccount The account to transfer funds from.
     * @param toAccount The account to transfer funds to.
     * @param amount The amount of funds to transfer.
     * @return The transaction reference number.
     */
    private String internalTransfer(Account fromAccount, Account toAccount, BigDecimal amount, String note) {

        fromAccount.setAvailableBalance(fromAccount.getAvailableBalance().subtract(amount));
        accountService.updateAccount(fromAccount.getAccountNumber(), fromAccount);

        toAccount.setAvailableBalance(toAccount.getAvailableBalance().add(amount));
        accountService.updateAccount(toAccount.getAccountNumber(), toAccount);

        String userNote = (note == null || note.trim().isEmpty()) ? "Chuyen khoa" : note.trim();
        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .accountId(fromAccount.getAccountNumber())
                        .transactionType("INTERNAL_TRANSFER")
                        .amount(amount.negate())
                        .description(userNote)
                        .build(),
                Transaction.builder()
                        .accountId(toAccount.getAccountNumber())
                        .transactionType("INTERNAL_TRANSFER")
                        .amount(amount)
                        .description(userNote).build());

        String transactionReference = UUID.randomUUID().toString();
        transactionService.makeInternalTransactions(transactions, transactionReference);
        return transactionReference;
    }

    /**
     * Retrieves the details of a fund transfer based on the given reference ID.
     *
     * @param referenceId The reference ID of the fund transfer.
     * @return The FundTransferDto containing the details of the fund transfer.
     * @throws ResourceNotFound if the fund transfer is not found.
     */
    @Override
    public FundTransferDto getTransferDetailsFromReferenceId(String referenceId) {

        return fundTransferRepository.findFundTransferByTransactionReference(referenceId)
                .map(fundTransferMapper::convertToDto)
                .orElseThrow(() -> new ResourceNotFound("Fund transfer not found", GlobalErrorCode.NOT_FOUND));
    }

    /**
     * Retrieves a list of fund transfers associated with the given account ID.
     *
     * @param accountId The ID of the account
     * @return A list of fund transfer DTOs
     */
    @Override
    public List<FundTransferDto> getAllTransfersByAccountId(String accountId) {

        return fundTransferMapper.convertToDtoList(fundTransferRepository.findFundTransferByFromAccount(accountId));
    }
}
