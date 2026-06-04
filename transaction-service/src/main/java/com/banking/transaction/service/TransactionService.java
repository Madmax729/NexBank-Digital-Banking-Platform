package com.banking.transaction.service;

import com.banking.transaction.client.AccountClient;
import com.banking.transaction.client.CurrencyClient;
import com.banking.transaction.client.LedgerClient;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.exception.TransactionException;
import com.banking.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final CurrencyClient currencyClient;
    private final LedgerClient ledgerClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Core fund transfer with ACID guarantees, idempotency, and double-entry ledger integration.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse transfer(TransferRequest request, String idempotencyKey) {
        log.info("Processing transfer: {} -> {}, amount: {}", 
                request.getSourceAccountId(), request.getDestinationAccountId(), request.getAmount());

        // Step 1: Check idempotency
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            String cachedResponse = redisTemplate.opsForValue().get("idempotency:" + idempotencyKey);
            if (cachedResponse != null) {
                log.info("Idempotent request detected, returning cached response for key: {}", idempotencyKey);
                Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    return mapToResponse(existing.get());
                }
            }
        }

        UUID sourceId = UUID.fromString(request.getSourceAccountId());
        UUID destId = UUID.fromString(request.getDestinationAccountId());

        if (sourceId.equals(destId)) {
            throw new TransactionException("Source and destination accounts cannot be the same");
        }

        // Step 2: Validate accounts
        Map<String, Object> sourceAccountResp = accountClient.getAccount(sourceId);
        Map<String, Object> destAccountResp = accountClient.getAccount(destId);

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceAccount = (Map<String, Object>) sourceAccountResp.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> destAccount = (Map<String, Object>) destAccountResp.get("data");

        if (!"ACTIVE".equals(sourceAccount.get("status"))) {
            throw new TransactionException("Source account is not active");
        }
        if (!"ACTIVE".equals(destAccount.get("status"))) {
            throw new TransactionException("Destination account is not active");
        }

        // Step 3: Check balance
        BigDecimal sourceBalance = new BigDecimal(sourceAccount.get("balance").toString());
        BigDecimal transferAmount = request.getAmount();

        String sourceCurrency = sourceAccount.get("currency").toString();
        String destCurrency = destAccount.get("currency").toString();
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = transferAmount;

        // Step 4: Currency conversion if needed
        if (!sourceCurrency.equals(destCurrency)) {
            try {
                Map<String, Object> conversionResp = currencyClient.convert(sourceCurrency, destCurrency, transferAmount);
                @SuppressWarnings("unchecked")
                Map<String, Object> conversionData = (Map<String, Object>) conversionResp.get("data");
                convertedAmount = new BigDecimal(conversionData.get("convertedAmount").toString());
                exchangeRate = new BigDecimal(conversionData.get("exchangeRate").toString());
            } catch (Exception e) {
                log.error("Currency conversion failed, using fallback", e);
                throw new TransactionException("Currency conversion service unavailable");
            }
        }

        if (sourceBalance.compareTo(transferAmount) < 0) {
            throw new TransactionException("Insufficient balance. Available: " + sourceBalance + " " + sourceCurrency);
        }

        // Step 5: Create transaction record
        String referenceNumber = generateReferenceNumber();
        Transaction transaction = Transaction.builder()
                .referenceNumber(referenceNumber)
                .sourceAccountId(sourceId)
                .destinationAccountId(destId)
                .amount(transferAmount)
                .sourceCurrency(sourceCurrency)
                .destinationCurrency(destCurrency)
                .exchangeRate(exchangeRate)
                .convertedAmount(convertedAmount)
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.PROCESSING)
                .description(request.getDescription())
                .idempotencyKey(idempotencyKey)
                .fraudStatus(Transaction.FraudStatus.PENDING)
                .build();

        transaction = transactionRepository.save(transaction);

        try {
            // Step 6: Create ledger entries (double-entry accounting)
            Map<String, Object> ledgerRequest = new HashMap<>();
            ledgerRequest.put("transactionId", transaction.getId().toString());
            ledgerRequest.put("sourceAccountId", sourceId.toString());
            ledgerRequest.put("destinationAccountId", destId.toString());
            ledgerRequest.put("amount", transferAmount.toString());
            ledgerRequest.put("convertedAmount", convertedAmount.toString());
            ledgerRequest.put("sourceCurrency", sourceCurrency);
            ledgerRequest.put("destinationCurrency", destCurrency);
            ledgerRequest.put("description", request.getDescription());
            ledgerClient.createLedgerEntries(ledgerRequest);

            // Step 7: Update balances
            BigDecimal newSourceBalance = sourceBalance.subtract(transferAmount);
            BigDecimal destBalance = new BigDecimal(destAccount.get("balance").toString());
            BigDecimal newDestBalance = destBalance.add(convertedAmount);

            accountClient.updateBalance(sourceId, Map.of("balance", newSourceBalance));
            accountClient.updateBalance(destId, Map.of("balance", newDestBalance));

            // Step 8: Mark transaction complete
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction = transactionRepository.save(transaction);

            // Step 9: Cache idempotency response
            if (idempotencyKey != null) {
                redisTemplate.opsForValue().set("idempotency:" + idempotencyKey,
                        transaction.getId().toString(), 24, TimeUnit.HOURS);
            }

            // Step 10: Publish Kafka events
            String transactionEvent = String.format(
                    "{\"eventType\":\"TRANSACTION_COMPLETED\",\"transactionId\":\"%s\",\"referenceNumber\":\"%s\"," +
                    "\"sourceAccountId\":\"%s\",\"destinationAccountId\":\"%s\",\"amount\":\"%s\"," +
                    "\"sourceCurrency\":\"%s\",\"destinationCurrency\":\"%s\",\"type\":\"%s\"," +
                    "\"timestamp\":\"%s\"}",
                    transaction.getId(), referenceNumber, sourceId, destId,
                    transferAmount, sourceCurrency, destCurrency, "TRANSFER", LocalDateTime.now());

            kafkaTemplate.send("transaction-created", transactionEvent);
            kafkaTemplate.send("audit-events", String.format(
                    "{\"action\":\"TRANSFER_COMPLETED\",\"transactionId\":\"%s\",\"amount\":\"%s\",\"timestamp\":\"%s\"}",
                    transaction.getId(), transferAmount, LocalDateTime.now()));
            kafkaTemplate.send("notification-events", String.format(
                    "{\"type\":\"TRANSACTION\",\"userId\":\"%s\",\"message\":\"Transfer of %s %s completed. Ref: %s\",\"timestamp\":\"%s\"}",
                    sourceAccount.get("userId"), transferAmount, sourceCurrency, referenceNumber, LocalDateTime.now()));

            log.info("Transfer completed successfully: {}", referenceNumber);
            return mapToResponse(transaction);

        } catch (Exception e) {
            // Rollback: Mark transaction as failed
            log.error("Transfer failed, rolling back: {}", e.getMessage());
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);

            kafkaTemplate.send("transaction-created", String.format(
                    "{\"eventType\":\"TRANSACTION_FAILED\",\"transactionId\":\"%s\",\"reason\":\"%s\",\"timestamp\":\"%s\"}",
                    transaction.getId(), e.getMessage(), LocalDateTime.now()));

            throw new TransactionException("Transfer failed: " + e.getMessage());
        }
    }

    @Transactional
    public TransactionResponse reverseTransaction(UUID transactionId) {
        log.info("Reversing transaction: {}", transactionId);

        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found: " + transactionId));

        if (original.getStatus() != Transaction.TransactionStatus.COMPLETED) {
            throw new TransactionException("Only completed transactions can be reversed");
        }

        // Create reversal transaction
        Transaction reversal = Transaction.builder()
                .referenceNumber(generateReferenceNumber())
                .sourceAccountId(original.getDestinationAccountId())
                .destinationAccountId(original.getSourceAccountId())
                .amount(original.getConvertedAmount() != null ? original.getConvertedAmount() : original.getAmount())
                .sourceCurrency(original.getDestinationCurrency() != null ? original.getDestinationCurrency() : original.getSourceCurrency())
                .destinationCurrency(original.getSourceCurrency())
                .type(Transaction.TransactionType.REVERSAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Reversal of transaction: " + original.getReferenceNumber())
                .reversedTransactionId(original.getId())
                .fraudStatus(Transaction.FraudStatus.CLEARED)
                .build();

        reversal = transactionRepository.save(reversal);

        // Update original transaction status
        original.setStatus(Transaction.TransactionStatus.REVERSED);
        transactionRepository.save(original);

        // Reverse ledger entries
        Map<String, Object> reversalReq = new HashMap<>();
        reversalReq.put("originalTransactionId", original.getId().toString());
        reversalReq.put("reversalTransactionId", reversal.getId().toString());
        ledgerClient.createReversalEntries(reversalReq);

        // Reverse account balances
        Map<String, Object> srcAcctResp = accountClient.getAccount(original.getSourceAccountId());
        Map<String, Object> destAcctResp = accountClient.getAccount(original.getDestinationAccountId());
        @SuppressWarnings("unchecked")
        Map<String, Object> srcAcct = (Map<String, Object>) srcAcctResp.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> destAcct = (Map<String, Object>) destAcctResp.get("data");

        BigDecimal srcBal = new BigDecimal(srcAcct.get("balance").toString());
        BigDecimal destBal = new BigDecimal(destAcct.get("balance").toString());

        accountClient.updateBalance(original.getSourceAccountId(),
                Map.of("balance", srcBal.add(original.getAmount())));
        accountClient.updateBalance(original.getDestinationAccountId(),
                Map.of("balance", destBal.subtract(original.getConvertedAmount() != null ? original.getConvertedAmount() : original.getAmount())));

        kafkaTemplate.send("audit-events", String.format(
                "{\"action\":\"TRANSACTION_REVERSED\",\"originalId\":\"%s\",\"reversalId\":\"%s\",\"timestamp\":\"%s\"}",
                original.getId(), reversal.getId(), LocalDateTime.now()));

        log.info("Transaction reversed: {} -> {}", original.getReferenceNumber(), reversal.getReferenceNumber());
        return mapToResponse(reversal);
    }

    public TransactionResponse getTransaction(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionException("Transaction not found"));
        return mapToResponse(transaction);
    }

    public TransactionResponse getByReference(String referenceNumber) {
        Transaction transaction = transactionRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new TransactionException("Transaction not found"));
        return mapToResponse(transaction);
    }

    public Page<TransactionResponse> getTransactionHistory(List<UUID> accountIds, Pageable pageable) {
        return transactionRepository.findByAccountIds(accountIds, pageable)
                .map(this::mapToResponse);
    }

    public Page<TransactionResponse> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable).map(this::mapToResponse);
    }

    public Page<TransactionResponse> getByStatus(Transaction.TransactionStatus status, Pageable pageable) {
        return transactionRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Transactional
    public void updateFraudStatus(UUID transactionId, Transaction.FraudStatus fraudStatus) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionException("Transaction not found"));
        transaction.setFraudStatus(fraudStatus);
        transactionRepository.save(transaction);
    }

    private String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + new Random().nextInt(1000);
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId().toString())
                .referenceNumber(t.getReferenceNumber())
                .sourceAccountId(t.getSourceAccountId().toString())
                .destinationAccountId(t.getDestinationAccountId().toString())
                .amount(t.getAmount())
                .sourceCurrency(t.getSourceCurrency())
                .destinationCurrency(t.getDestinationCurrency())
                .exchangeRate(t.getExchangeRate())
                .convertedAmount(t.getConvertedAmount())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .fraudStatus(t.getFraudStatus().name())
                .description(t.getDescription())
                .failureReason(t.getFailureReason())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
