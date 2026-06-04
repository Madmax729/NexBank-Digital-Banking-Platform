package com.banking.fraud.repository;

import com.banking.fraud.entity.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlert.FraudStatus status, Pageable pageable);
    Page<FraudAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(FraudAlert.FraudStatus status);
}
