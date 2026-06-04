package com.banking.currency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${currency.api.key:}")
    private String apiKey;

    @Value("${currency.api.base-url:https://v6.exchangerate-api.com/v6}")
    private String apiBaseUrl;

    // Static fallback rates against USD (used when API is unavailable or key is empty)
    private static final Map<String, BigDecimal> FALLBACK_RATES = Map.of(
            "USD", BigDecimal.ONE,
            "INR", new BigDecimal("83.50"),
            "EUR", new BigDecimal("0.92"),
            "GBP", new BigDecimal("0.79")
    );

    /**
     * Fetches exchange rates — tries live API first, falls back to static rates.
     * Results are cached in Redis for 4 hours.
     */
    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> getBaseRates() {
        // Check Redis cache first
        String cached = redisTemplate.opsForValue().get("live_rates:all");
        if (cached != null) {
            try {
                Map<String, Object> rates = objectMapper.readValue(cached, Map.class);
                return convertToRates(rates);
            } catch (Exception e) {
                log.warn("Failed to parse cached rates, fetching fresh: {}", e.getMessage());
            }
        }

        // Try live API if key is configured
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                String url = apiBaseUrl + "/" + apiKey + "/latest/USD";
                log.info("Fetching live exchange rates from ExchangeRate-API");
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);

                if (response != null && "success".equals(response.get("result"))) {
                    Map<String, Object> conversionRates = (Map<String, Object>) response.get("conversion_rates");
                    if (conversionRates != null) {
                        // Cache for 4 hours (free tier updates daily anyway)
                        String json = objectMapper.writeValueAsString(conversionRates);
                        redisTemplate.opsForValue().set("live_rates:all", json, 4, TimeUnit.HOURS);
                        log.info("Live exchange rates cached successfully ({} currencies)", conversionRates.size());
                        return convertToRates(conversionRates);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch live rates from API, using fallback: {}", e.getMessage());
            }
        } else {
            log.debug("No API key configured, using fallback rates");
        }

        return FALLBACK_RATES;
    }

    /**
     * Converts raw API response map to BigDecimal rate map.
     */
    private Map<String, BigDecimal> convertToRates(Map<String, Object> rawRates) {
        Map<String, BigDecimal> rates = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawRates.entrySet()) {
            try {
                rates.put(entry.getKey(), new BigDecimal(entry.getValue().toString()));
            } catch (NumberFormatException e) {
                // Skip non-numeric entries
            }
        }
        return rates;
    }

    public Map<String, Object> getAllRates() {
        Map<String, BigDecimal> baseRates = getBaseRates();
        String[] currencies = {"INR", "USD", "EUR", "GBP"};

        Map<String, Object> rates = new HashMap<>();
        for (String from : currencies) {
            Map<String, BigDecimal> toRates = new HashMap<>();
            for (String to : currencies) {
                if (!from.equals(to)) {
                    toRates.put(to, getExchangeRate(from, to));
                }
            }
            rates.put(from, toRates);
        }

        // Add metadata about data source
        boolean isLive = apiKey != null && !apiKey.isEmpty()
                && redisTemplate.opsForValue().get("live_rates:all") != null;
        rates.put("_source", isLive ? "LIVE" : "STATIC_FALLBACK");

        return rates;
    }

    public BigDecimal getExchangeRate(String from, String to) {
        String cacheKey = "exchange_rate:" + from + ":" + to;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return new BigDecimal(cached);
        }

        Map<String, BigDecimal> baseRates = getBaseRates();
        BigDecimal fromRate = baseRates.getOrDefault(from, BigDecimal.ONE);
        BigDecimal toRate = baseRates.getOrDefault(to, BigDecimal.ONE);
        BigDecimal rate = toRate.divide(fromRate, 8, RoundingMode.HALF_UP);

        redisTemplate.opsForValue().set(cacheKey, rate.toString(), 1, TimeUnit.HOURS);
        return rate;
    }

    public Map<String, Object> convert(String from, String to, BigDecimal amount) {
        BigDecimal rate = getExchangeRate(from, to);
        BigDecimal converted = amount.multiply(rate).setScale(4, RoundingMode.HALF_UP);

        return Map.of(
                "from", from, "to", to,
                "originalAmount", amount, "convertedAmount", converted,
                "exchangeRate", rate
        );
    }
}
