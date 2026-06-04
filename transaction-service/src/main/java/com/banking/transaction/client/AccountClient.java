package com.banking.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/api/user/accounts/{id}")
    Map<String, Object> getAccount(@PathVariable("id") UUID id);

    @GetMapping("/api/user/accounts/{id}/balance")
    Map<String, Object> getBalance(@PathVariable("id") UUID id);

    @PutMapping("/internal/accounts/{id}/balance")
    void updateBalance(@PathVariable("id") UUID id, @RequestBody Map<String, BigDecimal> body);
}
