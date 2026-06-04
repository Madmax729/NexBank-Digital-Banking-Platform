package com.banking.transaction.controller;

import com.banking.transaction.dto.ApiResponse;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.dto.TransferRequest;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Fund transfer and transaction management")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/user/transfer")
    @Operation(summary = "Transfer funds between accounts")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        TransactionResponse response = transactionService.transfer(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer completed", response));
    }

    @GetMapping("/user/history")
    @Operation(summary = "Get transaction history")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @RequestParam List<String> accountIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<UUID> uuids = accountIds.stream().map(UUID::fromString).collect(Collectors.toList());
        Page<TransactionResponse> history = transactionService.getTransactionHistory(
                uuids, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("History retrieved", history));
    }

    @GetMapping("/user/transfer/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Retrieved", transactionService.getTransaction(id)));
    }

    @PostMapping("/user/refund/{transactionId}")
    @Operation(summary = "Request a refund")
    public ResponseEntity<ApiResponse<TransactionResponse>> refund(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(ApiResponse.success("Reversed", transactionService.reverseTransaction(transactionId)));
    }

    @GetMapping("/admin/transactions")
    @Operation(summary = "Get all transactions (Admin)")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Page<TransactionResponse> transactions;
        if (status != null) {
            transactions = transactionService.getByStatus(Transaction.TransactionStatus.valueOf(status),
                    PageRequest.of(page, size, Sort.by("createdAt").descending()));
        } else {
            transactions = transactionService.getAllTransactions(
                    PageRequest.of(page, size, Sort.by("createdAt").descending()));
        }
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", transactions));
    }

    @PostMapping("/admin/transactions/{id}/reverse")
    @Operation(summary = "Reverse a transaction (Admin)")
    public ResponseEntity<ApiResponse<TransactionResponse>> adminReverse(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Reversed", transactionService.reverseTransaction(id)));
    }

    @PutMapping("/internal/transactions/{id}/fraud-status")
    public ResponseEntity<Void> updateFraudStatus(@PathVariable UUID id, @RequestParam String fraudStatus) {
        transactionService.updateFraudStatus(id, Transaction.FraudStatus.valueOf(fraudStatus));
        return ResponseEntity.ok().build();
    }
}
