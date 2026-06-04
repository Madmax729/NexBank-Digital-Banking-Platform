package com.banking.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "ledger-service")
public interface LedgerClient {

    @PostMapping("/api/ledger/entries")
    Map<String, Object> createLedgerEntries(@RequestBody Map<String, Object> request);

    @PostMapping("/api/ledger/reversal")
    Map<String, Object> createReversalEntries(@RequestBody Map<String, Object> request);
}
