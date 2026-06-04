package com.banking.admin.controller;

import com.banking.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(Map.of("success", true, "data", dashboardService.getDashboardAnalytics()));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
                "status", "UP",
                "services", Map.of(
                        "auth-service", "UP", "account-service", "UP",
                        "transaction-service", "UP", "ledger-service", "UP",
                        "currency-service", "UP", "fraud-service", "UP",
                        "notification-service", "UP", "audit-service", "UP"
                )
        )));
    }
}
