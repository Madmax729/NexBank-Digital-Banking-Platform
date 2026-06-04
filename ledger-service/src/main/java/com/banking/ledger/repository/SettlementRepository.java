package com.banking.ledger.repository;

import com.banking.ledger.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    Optional<Settlement> findBySettlementDate(LocalDate date);
    Page<Settlement> findAllByOrderBySettlementDateDesc(Pageable pageable);
}
