package com.banking.fraud.service;

import com.banking.fraud.entity.FraudAlert;
import com.banking.fraud.repository.FraudAlertRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fraud.large-amount-threshold:100000}")
    private BigDecimal largeAmountThreshold;

    @Value("${fraud.cross-currency-threshold:50000}")
    private BigDecimal crossCurrencyThreshold;

    @KafkaListener(topics = "transaction-created", groupId = "fraud-detection-group")
    public void analyzeTransaction(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("eventType").asText();

            if (!"TRANSACTION_COMPLETED".equals(eventType)) return;

            UUID transactionId = UUID.fromString(event.get("transactionId").asText());
            BigDecimal amount = new BigDecimal(event.get("amount").asText());
            UUID sourceAccountId = UUID.fromString(event.get("sourceAccountId").asText());

            List<String> violations = new ArrayList<>();

            // Rule 1: Large amount threshold
            if (amount.compareTo(largeAmountThreshold) > 0) {
                violations.add("LARGE_AMOUNT");
            }

            // Rule 2: Cross-currency rapid transfer
            String srcCurrency = event.get("sourceCurrency").asText();
            String destCurrency = event.get("destinationCurrency").asText();
            if (!srcCurrency.equals(destCurrency) && amount.compareTo(crossCurrencyThreshold) > 0) {
                violations.add("SUSPICIOUS_CROSS_CURRENCY");
            }

            // Create fraud alerts for violations
            for (String rule : violations) {
                FraudAlert alert = FraudAlert.builder()
                        .transactionId(transactionId)
                        .accountId(sourceAccountId)
                        .amount(amount)
                        .rule(rule)
                        .details(String.format("Rule: %s triggered for transaction %s, amount: %s %s",
                                rule, transactionId, amount, srcCurrency))
                        .status(FraudAlert.FraudStatus.FLAGGED)
                        .build();
                fraudAlertRepository.save(alert);

                log.warn("FRAUD ALERT: {} for transaction {}", rule, transactionId);

                kafkaTemplate.send("fraud-detected", String.format(
                        "{\"transactionId\":\"%s\",\"rule\":\"%s\",\"amount\":\"%s\",\"status\":\"FLAGGED\",\"timestamp\":\"%s\"}",
                        transactionId, rule, amount, LocalDateTime.now()));
            }

            if (violations.isEmpty()) {
                log.info("Transaction {} passed fraud checks", transactionId);
            }

        } catch (Exception e) {
            log.error("Error analyzing transaction for fraud: {}", e.getMessage());
        }
    }

    public Page<FraudAlert> getAlerts(FraudAlert.FraudStatus status, Pageable pageable) {
        if (status != null) {
            return fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public FraudAlert clearAlert(UUID alertId, String adminId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setStatus(FraudAlert.FraudStatus.CLEARED);
        alert.setResolvedBy(adminId);
        alert.setResolvedAt(LocalDateTime.now());
        return fraudAlertRepository.save(alert);
    }

    @Transactional
    public FraudAlert blockAlert(UUID alertId, String adminId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setStatus(FraudAlert.FraudStatus.BLOCKED);
        alert.setResolvedBy(adminId);
        alert.setResolvedAt(LocalDateTime.now());
        return fraudAlertRepository.save(alert);
    }

    public long countFlagged() {
        return fraudAlertRepository.countByStatus(FraudAlert.FraudStatus.FLAGGED);
    }
}
