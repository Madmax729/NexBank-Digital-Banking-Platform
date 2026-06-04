package com.banking.account.repository;

import com.banking.account.entity.UpiId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpiIdRepository extends JpaRepository<UpiId, UUID> {
    Optional<UpiId> findByUpiId(String upiId);
    boolean existsByUpiId(String upiId);
    Optional<UpiId> findByUpiIdAndActiveTrue(String upiId);
}
