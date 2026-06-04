package com.banking.account.repository;

import com.banking.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserId(UUID userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") UUID id);

    Page<Account> findByUserId(UUID userId, Pageable pageable);

    boolean existsByAccountNumber(String accountNumber);

    long countByUserId(UUID userId);

    @Query("SELECT a FROM Account a WHERE a.status = :status")
    Page<Account> findByStatus(@Param("status") Account.AccountStatus status, Pageable pageable);
}
