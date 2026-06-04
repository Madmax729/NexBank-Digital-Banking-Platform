package com.banking.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private String id;
    private String referenceNumber;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String sourceCurrency;
    private String destinationCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private String type;
    private String status;
    private String fraudStatus;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
