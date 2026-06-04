package com.banking.fraud.controller;

import com.banking.fraud.entity.FraudAlert;
import com.banking.fraud.service.FraudDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/fraud-alerts")
@RequiredArgsConstructor
@Tag(name = "Fraud Detection", description = "Fraud monitoring and management")
public class FraudAlertController {

    private final FraudDetectionService fraudDetectionService;

    @GetMapping
    @Operation(summary = "Get fraud alerts")
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        FraudAlert.FraudStatus fraudStatus = status != null ? FraudAlert.FraudStatus.valueOf(status) : null;
        Page<FraudAlert> alerts = fraudDetectionService.getAlerts(fraudStatus, PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of("success", true, "data", alerts));
    }

    @PutMapping("/{id}/clear")
    @Operation(summary = "Clear a fraud alert")
    public ResponseEntity<Map<String, Object>> clear(@PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String adminId) {
        FraudAlert alert = fraudDetectionService.clearAlert(id, adminId);
        return ResponseEntity.ok(Map.of("success", true, "data", alert));
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block/confirm a fraud alert")
    public ResponseEntity<Map<String, Object>> block(@PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String adminId) {
        FraudAlert alert = fraudDetectionService.blockAlert(id, adminId);
        return ResponseEntity.ok(Map.of("success", true, "data", alert));
    }
}
