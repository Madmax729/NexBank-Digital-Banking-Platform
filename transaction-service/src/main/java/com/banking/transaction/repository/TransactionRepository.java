package com.banking.transaction.repository;

import com.banking.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceAccountId = :accountId OR t.destinationAccountId = :accountId) " +
           "AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIdAndStatus(@Param("accountId") UUID accountId,
                                                @Param("status") Transaction.TransactionStatus status,
                                                Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceAccountId IN :accountIds OR t.destinationAccountId IN :accountIds ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountIds(@Param("accountIds") List<UUID> accountIds, Pageable pageable);

    Page<Transaction> findByStatus(Transaction.TransactionStatus status, Pageable pageable);

    Page<Transaction> findByFraudStatus(Transaction.FraudStatus fraudStatus, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end ORDER BY t.createdAt DESC")
    Page<Transaction> findByDateRange(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.sourceAccountId = :accountId AND t.createdAt > :since")
    long countRecentTransactions(@Param("accountId") UUID accountId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    long countByStatus(@Param("status") Transaction.TransactionStatus status);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = 'COMPLETED' AND t.createdAt BETWEEN :start AND :end")
    java.math.BigDecimal sumCompletedAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
