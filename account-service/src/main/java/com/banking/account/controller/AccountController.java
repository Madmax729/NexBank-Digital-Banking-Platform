package com.banking.account.controller;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.ApiResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.service.AccountService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "Bank account operations")
public class AccountController {

    private final AccountService accountService;

    // ==================== USER ENDPOINTS ====================

    @PostMapping("/user/accounts")
    @Operation(summary = "Create a new bank account")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    @GetMapping("/user/accounts")
    @Operation(summary = "Get all accounts for the authenticated user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getUserAccounts(
            @RequestHeader("X-User-Id") String userId) {
        List<AccountResponse> accounts = accountService.getUserAccounts(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved", accounts));
    }

    @GetMapping("/user/accounts/{id}")
    @Operation(summary = "Get account details by ID")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable UUID id) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success("Account retrieved", response));
    }

    @GetMapping("/user/accounts/{id}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance(@PathVariable UUID id) {
        BigDecimal balance = accountService.getBalance(id);
        AccountResponse account = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved",
                Map.of("balance", balance, "currency", account.getCurrency())));
    }

    @PostMapping("/user/accounts/{id}/upi")
    @Operation(summary = "Register a UPI ID for an account")
    public ResponseEntity<ApiResponse<String>> registerUpi(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String upiId = accountService.registerUpiId(id, body.get("upiId"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("UPI ID registered", upiId));
    }

    @GetMapping("/upi/resolve/{upiId}")
    @Operation(summary = "Resolve UPI ID to account details")
    public ResponseEntity<ApiResponse<AccountResponse>> resolveUpi(@PathVariable String upiId) {
        AccountResponse response = accountService.resolveUpiId(upiId);
        return ResponseEntity.ok(ApiResponse.success("UPI ID resolved", response));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @PutMapping("/admin/freeze-account/{id}")
    @Operation(summary = "Freeze a bank account (Admin)")
    public ResponseEntity<ApiResponse<AccountResponse>> freezeAccount(@PathVariable UUID id) {
        AccountResponse response = accountService.freezeAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Account frozen", response));
    }

    @PutMapping("/admin/unfreeze-account/{id}")
    @Operation(summary = "Unfreeze a bank account (Admin)")
    public ResponseEntity<ApiResponse<AccountResponse>> unfreezeAccount(@PathVariable UUID id) {
        AccountResponse response = accountService.unfreezeAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Account unfrozen", response));
    }

    @GetMapping("/admin/accounts")
    @Operation(summary = "Get all accounts (Admin)")
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AccountResponse> accounts = accountService.getAllAccounts(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved", accounts));
    }

    // ==================== INTERNAL ENDPOINTS (Service-to-Service) ====================

    @PutMapping("/internal/accounts/{id}/balance")
    @Operation(summary = "Update account balance (Internal)")
    public ResponseEntity<Void> updateBalance(
            @PathVariable UUID id,
            @RequestBody Map<String, BigDecimal> body) {
        accountService.updateBalance(id, body.get("balance"));
        return ResponseEntity.ok().build();
    }
}
