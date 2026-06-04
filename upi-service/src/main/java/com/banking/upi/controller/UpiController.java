package com.banking.upi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequestMapping("/api/user") @RequiredArgsConstructor @Slf4j
public class UpiController {

    private final AccountFeignClient accountClient;
    private final TransactionFeignClient transactionClient;

    @PostMapping("/upi/register")
    public ResponseEntity<Map<String, Object>> registerUpi(
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        String accountId = body.get("accountId");
        String upiId = body.get("upiId");
        Map<String, Object> result = accountClient.registerUpi(accountId, Map.of("upiId", upiId));
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PostMapping("/upi-pay")
    public ResponseEntity<Map<String, Object>> upiPay(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        String senderUpiId = body.get("senderUpiId").toString();
        String receiverUpiId = body.get("receiverUpiId").toString();
        String amount = body.get("amount").toString();

        // Resolve UPI IDs to accounts
        Map<String, Object> senderAccount = accountClient.resolveUpi(senderUpiId);
        Map<String, Object> receiverAccount = accountClient.resolveUpi(receiverUpiId);

        @SuppressWarnings("unchecked")
        Map<String, Object> senderData = (Map<String, Object>) senderAccount.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> receiverData = (Map<String, Object>) receiverAccount.get("data");

        // Execute transfer
        Map<String, Object> transferReq = Map.of(
                "sourceAccountId", senderData.get("id"),
                "destinationAccountId", receiverData.get("id"),
                "amount", amount,
                "description", "UPI Payment: " + senderUpiId + " → " + receiverUpiId
        );

        Map<String, Object> result = transactionClient.transfer(transferReq, idempotencyKey);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @GetMapping("/upi/qr/{upiId}")
    public ResponseEntity<Map<String, Object>> generateQr(@PathVariable String upiId) {
        return ResponseEntity.ok(Map.of("success", true, "data",
                Map.of("upiId", upiId, "qrData", "upi://pay?pa=" + upiId + "&pn=Banking&cu=INR")));
    }

    @FeignClient(name = "account-service")
    interface AccountFeignClient {
        @PostMapping("/api/user/accounts/{id}/upi")
        Map<String, Object> registerUpi(@PathVariable("id") String id, @RequestBody Map<String, String> body);

        @GetMapping("/api/upi/resolve/{upiId}")
        Map<String, Object> resolveUpi(@PathVariable("upiId") String upiId);
    }

    @FeignClient(name = "transaction-service")
    interface TransactionFeignClient {
        @PostMapping("/api/user/transfer")
        Map<String, Object> transfer(@RequestBody Map<String, Object> request,
                                      @RequestHeader(value = "X-Idempotency-Key", required = false) String key);
    }
}
