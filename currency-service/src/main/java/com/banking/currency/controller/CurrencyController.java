package com.banking.currency.controller;

import com.banking.currency.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
@Tag(name = "Currency", description = "Exchange rates and conversion")
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping("/rates")
    @Operation(summary = "Get all exchange rates")
    public ResponseEntity<Map<String, Object>> getRates() {
        return ResponseEntity.ok(Map.of("success", true, "data", currencyService.getAllRates()));
    }

    @GetMapping("/rates/{from}/{to}")
    @Operation(summary = "Get exchange rate between two currencies")
    public ResponseEntity<Map<String, Object>> getRate(@PathVariable String from, @PathVariable String to) {
        BigDecimal rate = currencyService.getExchangeRate(from.toUpperCase(), to.toUpperCase());
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("from", from, "to", to, "rate", rate)));
    }

    @GetMapping("/convert")
    @Operation(summary = "Convert currency")
    public ResponseEntity<Map<String, Object>> convert(
            @RequestParam String from, @RequestParam String to, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(Map.of("success", true,
                "data", currencyService.convert(from.toUpperCase(), to.toUpperCase(), amount)));
    }
}
