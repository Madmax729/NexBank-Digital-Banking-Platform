package com.banking.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "currency-service")
public interface CurrencyClient {

    @GetMapping("/api/currency/convert")
    Map<String, Object> convert(@RequestParam("from") String from,
                                 @RequestParam("to") String to,
                                 @RequestParam("amount") BigDecimal amount);

    @GetMapping("/api/currency/rates")
    Map<String, Object> getRates();
}
