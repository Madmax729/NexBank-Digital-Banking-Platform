package com.banking.ledger.controller;

import com.banking.ledger.entity.LedgerEntry;
import com.banking.ledger.entity.Settlement;
import com.banking.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Ledger", description = "Double-entry accounting ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/ledger/entries")
    @Operation(summary = "Create ledger entries (Internal)")
    public ResponseEntity<Map<String, String>> createEntries(@RequestBody Map<String, Object> request) {
        ledgerService.createLedgerEntries(request);
        return ResponseEntity.ok(Map.of("status", "CREATED"));
    }

    @PostMapping("/ledger/reversal")
    @Operation(summary = "Create reversal entries (Internal)")
    public ResponseEntity<Map<String, String>> createReversal(@RequestBody Map<String, Object> request) {
        ledgerService.createReversalEntries(request);
        return ResponseEntity.ok(Map.of("status", "REVERSED"));
    }

    @GetMapping("/ledger/balance/{accountId}")
    @Operation(summary = "Get ledger-derived balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable UUID accountId) {
        BigDecimal balance = ledgerService.getAccountBalance(accountId);
        return ResponseEntity.ok(Map.of("accountId", accountId.toString(), "balance", balance));
    }

    @GetMapping("/ledger/entries/{accountId}")
    @Operation(summary = "Get ledger entries for account")
    public ResponseEntity<Page<LedgerEntry>> getEntries(@PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ledgerService.getAccountEntries(accountId, PageRequest.of(page, size)));
    }

    @GetMapping("/admin/ledger/reconciliation")
    @Operation(summary = "Get ledger reconciliation (Admin)")
    public ResponseEntity<Map<String, Object>> getReconciliation() {
        return ResponseEntity.ok(ledgerService.getReconciliation());
    }

    @PostMapping("/admin/ledger/settle/{date}")
    @Operation(summary = "Perform daily settlement (Admin)")
    public ResponseEntity<Settlement> performSettlement(@PathVariable String date) {
        return ResponseEntity.ok(ledgerService.performSettlement(LocalDate.parse(date)));
    }

    @GetMapping("/admin/ledger/settlements")
    @Operation(summary = "Get settlement reports (Admin)")
    public ResponseEntity<Page<Settlement>> getSettlements(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ledgerService.getSettlements(PageRequest.of(page, size)));
    }
}
