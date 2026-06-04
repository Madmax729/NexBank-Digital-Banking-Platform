package com.banking.ledger.service;

import com.banking.ledger.entity.JournalEntry;
import com.banking.ledger.entity.LedgerEntry;
import com.banking.ledger.entity.Settlement;
import com.banking.ledger.repository.JournalEntryRepository;
import com.banking.ledger.repository.LedgerEntryRepository;
import com.banking.ledger.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final SettlementRepository settlementRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Creates double-entry ledger entries for a transaction.
     * DEBIT the source account, CREDIT the destination account.
     * Entries are IMMUTABLE once created.
     */
    @Transactional
    public void createLedgerEntries(Map<String, Object> request) {
        UUID transactionId = UUID.fromString(request.get("transactionId").toString());
        UUID sourceAccountId = UUID.fromString(request.get("sourceAccountId").toString());
        UUID destAccountId = UUID.fromString(request.get("destinationAccountId").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        BigDecimal convertedAmount = request.containsKey("convertedAmount") ?
                new BigDecimal(request.get("convertedAmount").toString()) : amount;
        String sourceCurrency = request.get("sourceCurrency").toString();
        String destCurrency = request.containsKey("destinationCurrency") ?
                request.get("destinationCurrency").toString() : sourceCurrency;
        String description = request.containsKey("description") ?
                String.valueOf(request.get("description")) : "Fund Transfer";

        log.info("Creating double-entry ledger entries for transaction: {}", transactionId);

        // Create Journal Entry
        JournalEntry journal = JournalEntry.builder()
                .transactionId(transactionId)
                .description(description)
                .status(JournalEntry.JournalStatus.POSTED)
                .build();
        journal = journalEntryRepository.save(journal);

        // Calculate running balances
        BigDecimal sourceBalance = ledgerEntryRepository.calculateBalance(sourceAccountId);
        if (sourceBalance == null) sourceBalance = BigDecimal.ZERO;
        BigDecimal destBalance = ledgerEntryRepository.calculateBalance(destAccountId);
        if (destBalance == null) destBalance = BigDecimal.ZERO;

        // DEBIT entry (source account — money goes out)
        LedgerEntry debitEntry = LedgerEntry.builder()
                .journalEntry(journal)
                .transactionId(transactionId)
                .accountId(sourceAccountId)
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(amount)
                .currency(sourceCurrency)
                .balanceAfter(sourceBalance.subtract(amount))
                .reference("TXN-" + transactionId.toString().substring(0, 8))
                .build();

        // CREDIT entry (destination account — money comes in)
        LedgerEntry creditEntry = LedgerEntry.builder()
                .journalEntry(journal)
                .transactionId(transactionId)
                .accountId(destAccountId)
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(convertedAmount)
                .currency(destCurrency)
                .balanceAfter(destBalance.add(convertedAmount))
                .reference("TXN-" + transactionId.toString().substring(0, 8))
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        log.info("Ledger entries created - DEBIT: {} {}, CREDIT: {} {}",
                amount, sourceCurrency, convertedAmount, destCurrency);

        kafkaTemplate.send("audit-events", String.format(
                "{\"action\":\"LEDGER_ENTRIES_CREATED\",\"transactionId\":\"%s\",\"debitAmount\":\"%s\",\"creditAmount\":\"%s\",\"timestamp\":\"%s\"}",
                transactionId, amount, convertedAmount, LocalDateTime.now()));
    }

    /**
     * Creates reversal entries for a transaction.
     */
    @Transactional
    public void createReversalEntries(Map<String, Object> request) {
        UUID originalTxnId = UUID.fromString(request.get("originalTransactionId").toString());
        UUID reversalTxnId = UUID.fromString(request.get("reversalTransactionId").toString());

        var originalEntries = ledgerEntryRepository.findByTransactionId(originalTxnId);
        var originalJournals = journalEntryRepository.findByTransactionId(originalTxnId);

        // Mark original journal as REVERSED
        originalJournals.forEach(j -> {
            j.setStatus(JournalEntry.JournalStatus.REVERSED);
            journalEntryRepository.save(j);
        });

        // Create reversal journal
        JournalEntry reversalJournal = JournalEntry.builder()
                .transactionId(reversalTxnId)
                .description("Reversal of transaction: " + originalTxnId)
                .status(JournalEntry.JournalStatus.POSTED)
                .build();
        reversalJournal = journalEntryRepository.save(reversalJournal);

        // Create opposite entries
        for (LedgerEntry original : originalEntries) {
            LedgerEntry.EntryType reversedType = original.getEntryType() == LedgerEntry.EntryType.DEBIT ?
                    LedgerEntry.EntryType.CREDIT : LedgerEntry.EntryType.DEBIT;

            BigDecimal currentBalance = ledgerEntryRepository.calculateBalance(original.getAccountId());
            if (currentBalance == null) currentBalance = BigDecimal.ZERO;

            BigDecimal newBalance = reversedType == LedgerEntry.EntryType.CREDIT ?
                    currentBalance.add(original.getAmount()) : currentBalance.subtract(original.getAmount());

            LedgerEntry reversal = LedgerEntry.builder()
                    .journalEntry(reversalJournal)
                    .transactionId(reversalTxnId)
                    .accountId(original.getAccountId())
                    .entryType(reversedType)
                    .amount(original.getAmount())
                    .currency(original.getCurrency())
                    .balanceAfter(newBalance)
                    .reference("REV-" + originalTxnId.toString().substring(0, 8))
                    .build();
            ledgerEntryRepository.save(reversal);
        }

        log.info("Reversal ledger entries created for transaction: {}", originalTxnId);
    }

    public BigDecimal getAccountBalance(UUID accountId) {
        BigDecimal balance = ledgerEntryRepository.calculateBalance(accountId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    public Page<LedgerEntry> getAccountEntries(UUID accountId, Pageable pageable) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
    }

    /**
     * Daily settlement — verifies total debits == total credits
     */
    @Transactional
    public Settlement performSettlement(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        BigDecimal totalDebits = ledgerEntryRepository.sumByTypeAndDateRange(
                LedgerEntry.EntryType.DEBIT, start, end);
        BigDecimal totalCredits = ledgerEntryRepository.sumByTypeAndDateRange(
                LedgerEntry.EntryType.CREDIT, start, end);
        Long txnCount = ledgerEntryRepository.countTransactionsBetween(start, end);

        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;
        if (txnCount == null) txnCount = 0L;

        Settlement.SettlementStatus status = totalDebits.compareTo(totalCredits) == 0 ?
                Settlement.SettlementStatus.RECONCILED : Settlement.SettlementStatus.DISCREPANCY;

        Settlement settlement = Settlement.builder()
                .settlementDate(date)
                .totalDebits(totalDebits)
                .totalCredits(totalCredits)
                .transactionCount(txnCount)
                .status(status)
                .reconciledAt(status == Settlement.SettlementStatus.RECONCILED ? LocalDateTime.now() : null)
                .build();

        settlement = settlementRepository.save(settlement);
        log.info("Settlement for {}: Debits={}, Credits={}, Status={}",
                date, totalDebits, totalCredits, status);

        kafkaTemplate.send("audit-events", String.format(
                "{\"action\":\"SETTLEMENT_COMPLETED\",\"date\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\"}",
                date, status, LocalDateTime.now()));

        return settlement;
    }

    public Map<String, Object> getReconciliation() {
        BigDecimal totalDebits = ledgerEntryRepository.sumByTypeAndDateRange(
                LedgerEntry.EntryType.DEBIT, LocalDateTime.MIN, LocalDateTime.now());
        BigDecimal totalCredits = ledgerEntryRepository.sumByTypeAndDateRange(
                LedgerEntry.EntryType.CREDIT, LocalDateTime.MIN, LocalDateTime.now());
        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;

        boolean balanced = totalDebits.compareTo(totalCredits) == 0;
        return Map.of("totalDebits", totalDebits, "totalCredits", totalCredits,
                "balanced", balanced, "difference", totalDebits.subtract(totalCredits));
    }

    public Page<Settlement> getSettlements(Pageable pageable) {
        return settlementRepository.findAllByOrderBySettlementDateDesc(pageable);
    }
}
