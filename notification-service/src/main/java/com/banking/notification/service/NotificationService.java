package com.banking.notification.service;

import com.banking.notification.entity.Notification;
import com.banking.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification-events", groupId = "notification-group")
    public void handleNotificationEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String userId = event.has("userId") ? event.get("userId").asText() : "system";
            String type = event.has("type") ? event.get("type").asText() : "GENERAL";
            String msg = event.has("message") ? event.get("message").asText() : "Notification";

            Notification notification = Notification.builder()
                    .userId(userId).type(type).message(msg).build();
            notification = notificationRepository.save(notification);

            // Push via WebSocket
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
            log.info("Notification sent to user {}: {}", userId, type);

            // Simulate email
            log.info("[EMAIL] To: user-{}, Subject: {}, Body: {}", userId, type, msg);
        } catch (Exception e) {
            log.error("Error processing notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "fraud-detected", groupId = "notification-fraud-group")
    public void handleFraudAlert(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String txnId = event.get("transactionId").asText();
            String rule = event.get("rule").asText();

            Notification notification = Notification.builder()
                    .userId("admin")
                    .type("FRAUD_ALERT")
                    .message("Fraud detected: " + rule + " for transaction " + txnId)
                    .build();
            notificationRepository.save(notification);
            messagingTemplate.convertAndSend("/topic/admin/alerts", notification);
        } catch (Exception e) {
            log.error("Error processing fraud notification: {}", e.getMessage());
        }
    }

    public Page<Notification> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
