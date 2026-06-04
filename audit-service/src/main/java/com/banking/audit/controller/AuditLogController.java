package com.banking.audit.controller;

import com.banking.audit.entity.AuditLog;
import com.banking.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/admin/audit-logs") @RequiredArgsConstructor
public class AuditLogController {
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(required = false) String action, @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.getLogs(action, userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(Map.of("success", true, "data", logs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLog(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("success", true, "data", auditService.getLog(id)));
    }
}
