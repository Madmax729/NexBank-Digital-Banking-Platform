package com.banking.ledger.repository;

import com.banking.ledger.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    Page<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
    List<LedgerEntry> findByTransactionId(UUID transactionId);

    @Query("SELECT SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE 0 END) - " +
           "SUM(CASE WHEN e.entryType = 'DEBIT' THEN e.amount ELSE 0 END) FROM LedgerEntry e WHERE e.accountId = :accountId")
    BigDecimal calculateBalance(@Param("accountId") UUID accountId);

    @Query("SELECT SUM(e.amount) FROM LedgerEntry e WHERE e.entryType = :type AND e.createdAt BETWEEN :start AND :end")
    BigDecimal sumByTypeAndDateRange(@Param("type") LedgerEntry.EntryType type,
                                     @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT e.transactionId) FROM LedgerEntry e WHERE e.createdAt BETWEEN :start AND :end")
    Long countTransactionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
