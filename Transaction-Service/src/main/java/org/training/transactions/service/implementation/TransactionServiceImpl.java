package org.training.transactions.service.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.training.transactions.exception.AccountStatusException;
import org.training.transactions.exception.GlobalErrorCode;
import org.training.transactions.exception.InsufficientBalance;
import org.training.transactions.exception.ResourceNotFound;
import org.training.transactions.external.AccountService;
import org.training.transactions.model.TransactionStatus;
import org.training.transactions.model.TransactionType;
import org.training.transactions.model.dto.TransactionDto;
import org.training.transactions.model.entity.Transaction;
import org.training.transactions.model.external.Account;
import org.training.transactions.model.external.AccountRecipient;
import org.training.transactions.model.mapper.TransactionMapper;
import org.training.transactions.model.response.Response;
import org.training.transactions.model.response.TransactionReceiptResponse;
import org.training.transactions.model.response.TransactionRequest;
import org.training.transactions.repository.TransactionRepository;
import org.training.transactions.service.TransactionService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    private final TransactionMapper transactionMapper = new TransactionMapper();

    @Value("${spring.application.ok}")
    private String ok;

    /**
     * Adds a transaction based on the provided TransactionDto.
     *
     * @param  transactionDto  the TransactionDto object containing the transaction details
     * @return                 a Response object indicating the success of the transaction
     * @throws ResourceNotFound     if the requested account is not found on the server
     * @throws AccountStatusException     if the account is inactive or closed
     * @throws InsufficientBalance     if there is insufficient balance in the account
     */
    @Override
    public Response addTransaction(TransactionDto transactionDto) {

        ResponseEntity<Account> response = accountService.readByAccountNumber(transactionDto.getAccountId());
        if (Objects.isNull(response.getBody())){
            throw new ResourceNotFound("Requested account not found on the server", GlobalErrorCode.NOT_FOUND);
        }
        Account account = response.getBody();
        Transaction transaction = transactionMapper.convertToEntity(transactionDto);
        if(transactionDto.getTransactionType().equals(TransactionType.DEPOSIT.toString())) {
            account.setAvailableBalance(account.getAvailableBalance().add(transactionDto.getAmount()));
        } else if (transactionDto.getTransactionType().equals(TransactionType.WITHDRAWAL.toString())) {
            if(!account.getAccountStatus().equals("ACTIVE")){
                log.error("account is either inactive/closed, cannot process the transaction");
                throw new AccountStatusException("account is inactive or closed");
            }
            if(account.getAvailableBalance().compareTo(transactionDto.getAmount()) < 0){
                log.error("insufficient balance in the account");
                throw new InsufficientBalance("Insufficient balance in the account");
            }
            transaction.setAmount(transactionDto.getAmount().negate());
            account.setAvailableBalance(account.getAvailableBalance().subtract(transactionDto.getAmount()));
        }

        transaction.setTransactionType(TransactionType.valueOf(transactionDto.getTransactionType()));
        transaction.setComments(transactionDto.getDescription());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setReferenceId(UUID.randomUUID().toString());

        accountService.updateAccount(transactionDto.getAccountId(), account);
        transactionRepository.save(transaction);

        return Response.builder()
                .message("Transaction completed successfully")
                .responseCode(ok).build();
    }

    /**
     * Completes the internal transaction by updating the status of each transaction
     * and saving them to the transaction repository.
     *
     * @param transactionDtos the list of transaction DTOs to be processed
     * @return a response indicating the completion of the transaction
     */
    @Override
    public Response internalTransaction(List<TransactionDto> transactionDtos, String transactionReference) {

        // Convert the list of transaction DTOs to entities
        List<Transaction> transactions = transactionMapper.convertToEntityList(transactionDtos);

        // Update the status of each transaction to 'COMPLETED'
        transactions.forEach(transaction -> {
            transaction.setTransactionType(TransactionType.INTERNAL_TRANSFER);
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setReferenceId(transactionReference);
        });

        // Save all the completed transactions to the transaction repository
        transactionRepository.saveAll(transactions);

        // Return the response indicating the completion of the transaction
        return Response.builder()
                .responseCode(ok)
                .message("Transaction completed successfully").build();
    }

    /**
     * Retrieves a list of transaction requests for a given account ID.
     *
     * @param accountId the ID of the account
     * @return a list of transaction requests
     */
    @Override
    public List<TransactionRequest> getTransaction(String accountId) {

        return transactionRepository.findTransactionByAccountIdOrderByTransactionDateDesc(accountId)
                .stream()
                .map(transaction -> toTransactionResponse(transaction, accountId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of TransactionRequests based on a transaction reference.
     *
     * @param transactionReference The reference ID of the transaction
     * @return List of TransactionRequests matching the transaction reference
     */
    @Override
    public List<TransactionRequest> getTransactionByTransactionReference(String transactionReference) {

        return transactionRepository.findTransactionByReferenceId(transactionReference)
                .stream()
                .map(transaction -> toTransactionResponse(transaction, transaction.getAccountId()))
                .collect(Collectors.toList());
    }

    @Override
    public TransactionReceiptResponse getTransactionReceiptByReference(String transactionReference) {
        List<Transaction> transactions = transactionRepository.findTransactionByReferenceId(transactionReference);
        if (transactions.isEmpty()) {
            throw new ResourceNotFound("Transaction receipt not found", GlobalErrorCode.NOT_FOUND);
        }
        return buildReceipt(transactions, null);
    }

    @Override
    public TransactionReceiptResponse getTransactionReceiptById(Long transactionId) {
        Transaction selectedTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFound("Transaction receipt not found", GlobalErrorCode.NOT_FOUND));
        List<Transaction> transactions = transactionRepository.findTransactionByReferenceId(selectedTransaction.getReferenceId());
        if (transactions.isEmpty()) {
            transactions = List.of(selectedTransaction);
        }
        return buildReceipt(transactions, null);
    }

    private TransactionRequest toTransactionResponse(Transaction transaction, String currentAccountId) {
        TransactionRequest transactionRequest = new TransactionRequest();
        BeanUtils.copyProperties(transaction, transactionRequest);
        transactionRequest.setTransactionStatus(transaction.getStatus().toString());
        transactionRequest.setLocalDateTime(transaction.getTransactionDate());
        transactionRequest.setTransactionType(transaction.getTransactionType().toString());
        transactionRequest.setSignedAmount(transaction.getAmount());
        transactionRequest.setBankName("NLU Banking");

        BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
        boolean moneyIn = amount.compareTo(BigDecimal.ZERO) > 0;
        String counterpartyAccount = resolveCounterpartyAccount(transaction, currentAccountId, moneyIn);

        transactionRequest.setDirection(moneyIn ? "IN" : "OUT");
        transactionRequest.setAmount(amount.abs());
        transactionRequest.setCounterpartyAccount(counterpartyAccount);
        transactionRequest.setCounterpartyName(counterpartyAccount == null || counterpartyAccount.isEmpty()
                ? ""
                : "Tai khoan " + counterpartyAccount);
        transactionRequest.setDisplayTitle(moneyIn ? "Nhan tien" : "Chuyen tien");
        transactionRequest.setDisplayMessage(buildDisplayMessage(moneyIn, counterpartyAccount));
        return transactionRequest;
    }

    private TransactionReceiptResponse buildReceipt(List<Transaction> transactions, Transaction selectedTransaction) {
        Transaction primary = selectedTransaction == null ? transactions.get(0) : selectedTransaction;
        Transaction debit = transactions.stream()
                .filter(transaction -> transaction.getAmount() != null
                        && transaction.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .findFirst()
                .orElse(null);
        Transaction credit = transactions.stream()
                .filter(transaction -> transaction.getAmount() != null
                        && transaction.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(null);

        String fromAccount = debit == null ? "" : debit.getAccountId();
        String toAccount = credit == null ? "" : credit.getAccountId();
        BigDecimal amount = resolveReceiptAmount(primary, debit, credit);
        boolean currentMoneyIn = primary.getAmount() != null && primary.getAmount().compareTo(BigDecimal.ZERO) > 0;
        String counterpartyAccount = currentMoneyIn ? fromAccount : toAccount;
        AccountRecipient recipient = findRecipient(toAccount);
        String recipientName = recipient == null ? fallbackAccountName(toAccount) : safe(recipient.getAccountHolderName());
        String recipientBank = recipient == null ? "NLU Banking" : safe(recipient.getBankName());

        return TransactionReceiptResponse.builder()
                .transactionId(primary.getTransactionId())
                .referenceId(primary.getReferenceId())
                .type(primary.getTransactionType() == null ? "" : primary.getTransactionType().toString())
                .amount(amount)
                .direction(currentMoneyIn ? "IN" : "OUT")
                .status(resolveStatus(transactions))
                .time(resolveTime(transactions))
                .description(resolveDescription(primary))
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .recipientName(recipientName)
                .recipientBank(recipientBank)
                .counterpartyAccount(counterpartyAccount)
                .counterpartyName(fallbackAccountName(counterpartyAccount))
                .build();
    }

    private BigDecimal resolveReceiptAmount(Transaction primary, Transaction debit, Transaction credit) {
        if (debit != null && debit.getAmount() != null) {
            return debit.getAmount().abs();
        }
        if (credit != null && credit.getAmount() != null) {
            return credit.getAmount().abs();
        }
        return primary.getAmount() == null ? BigDecimal.ZERO : primary.getAmount().abs();
    }

    private String resolveStatus(List<Transaction> transactions) {
        boolean hasPending = transactions.stream()
                .anyMatch(transaction -> TransactionStatus.PENDING.equals(transaction.getStatus()));
        return hasPending ? TransactionStatus.PENDING.toString() : TransactionStatus.COMPLETED.toString();
    }

    private LocalDateTime resolveTime(List<Transaction> transactions) {
        return transactions.stream()
                .map(Transaction::getTransactionDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private String resolveDescription(Transaction transaction) {
        String comments = transaction.getComments();
        if (comments == null || comments.trim().isEmpty()) {
            return "";
        }
        String marker = " | note: ";
        int noteIndex = comments.indexOf(marker);
        if (noteIndex >= 0) {
            return comments.substring(noteIndex + marker.length()).trim();
        }
        return comments.trim();
    }

    private AccountRecipient findRecipient(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return null;
        }
        try {
            ResponseEntity<AccountRecipient> response = accountService.readRecipient(accountNumber);
            return response.getBody();
        } catch (Exception exception) {
            log.warn("Unable to resolve recipient metadata for account {}: {}", accountNumber, exception.getMessage());
            return null;
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "" : value.trim();
    }

    private String fallbackAccountName(String accountNumber) {
        return accountNumber == null || accountNumber.trim().isEmpty()
                ? ""
                : "Tai khoan " + accountNumber.trim();
    }

    private String buildDisplayMessage(boolean moneyIn, String counterpartyAccount) {
        if (counterpartyAccount == null || counterpartyAccount.isEmpty()) {
            return moneyIn ? "Tien vao tai khoan" : "Tien ra khoi tai khoan";
        }
        return moneyIn
                ? "Nhan tien tu " + counterpartyAccount
                : "Chuyen tien den " + counterpartyAccount;
    }

    private String resolveCounterpartyAccount(Transaction transaction, String currentAccountId, boolean moneyIn) {
        String comments = transaction.getComments() == null ? "" : transaction.getComments();
        if (comments.contains(" to ")) {
            String value = comments.substring(comments.lastIndexOf(" to ") + 4).trim();
            return stripNonAccountSuffix(value);
        }
        if (comments.contains("from:")) {
            String value = comments.substring(comments.lastIndexOf("from:") + 5).trim();
            return stripNonAccountSuffix(value);
        }
        if (comments.contains(" from ")) {
            String value = comments.substring(comments.lastIndexOf(" from ") + 6).trim();
            return stripNonAccountSuffix(value);
        }
        return "";
    }

    private String stripNonAccountSuffix(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        int firstSpace = trimmed.indexOf(' ');
        return firstSpace < 0 ? trimmed : trimmed.substring(0, firstSpace);
    }
}
