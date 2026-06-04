package com.banking.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service @RequiredArgsConstructor @Slf4j
public class AdminDashboardService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // In-memory analytics counters
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong completedTransactions = new AtomicLong(0);
    private final AtomicLong failedTransactions = new AtomicLong(0);
    private final AtomicLong fraudAlerts = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> currencyUsage = new ConcurrentHashMap<>();

    @KafkaListener(topics = "transaction-created", groupId = "admin-monitoring-group")
    public void handleTransactionEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("eventType").asText();
            totalTransactions.incrementAndGet();

            if ("TRANSACTION_COMPLETED".equals(eventType)) {
                completedTransactions.incrementAndGet();
                String currency = event.has("sourceCurrency") ? event.get("sourceCurrency").asText() : "INR";
                currencyUsage.computeIfAbsent(currency, k -> new AtomicLong(0)).incrementAndGet();
            } else if ("TRANSACTION_FAILED".equals(eventType)) {
                failedTransactions.incrementAndGet();
            }

            // Push live update to admin dashboard
            messagingTemplate.convertAndSend("/topic/admin/live", Map.of(
                    "type", "TRANSACTION", "event", eventType,
                    "totalTransactions", totalTransactions.get(),
                    "completed", completedTransactions.get(),
                    "failed", failedTransactions.get()
            ));
        } catch (Exception e) {
            log.error("Error processing admin transaction event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "fraud-detected", groupId = "admin-monitoring-fraud-group")
    public void handleFraudEvent(String message) {
        fraudAlerts.incrementAndGet();
        messagingTemplate.convertAndSend("/topic/admin/alerts", Map.of(
                "type", "FRAUD_ALERT", "count", fraudAlerts.get(), "event", message
        ));
    }

    public Map<String, Object> getDashboardAnalytics() {
        return Map.of(
                "totalTransactions", totalTransactions.get(),
                "completedTransactions", completedTransactions.get(),
                "failedTransactions", failedTransactions.get(),
                "fraudAlerts", fraudAlerts.get(),
                "currencyUsage", currencyUsage,
                "successRate", totalTransactions.get() > 0 ?
                        (double) completedTransactions.get() / totalTransactions.get() * 100 : 0
        );
    }
}
