package com.banking.audit.service;

import com.banking.audit.entity.AuditLog;
import com.banking.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "audit-events", groupId = "audit-service-group")
    public void handleAuditEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            AuditLog auditLog = AuditLog.builder()
                    .action(event.has("action") ? event.get("action").asText() : "UNKNOWN")
                    .userId(event.has("userId") ? event.get("userId").asText() : null)
                    .entityId(event.has("transactionId") ? event.get("transactionId").asText() :
                              event.has("accountId") ? event.get("accountId").asText() : null)
                    .details(message)
                    .build();
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {}", auditLog.getAction());
        } catch (Exception e) {
            log.error("Error processing audit event: {}", e.getMessage());
        }
    }

    public Page<AuditLog> getLogs(String action, String userId, Pageable pageable) {
        if (action != null) return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
        if (userId != null) return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public AuditLog getLog(UUID id) {
        return auditLogRepository.findById(id).orElseThrow(() -> new RuntimeException("Audit log not found"));
    }
}
