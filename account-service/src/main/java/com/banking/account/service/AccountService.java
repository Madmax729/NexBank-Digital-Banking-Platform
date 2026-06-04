package com.banking.account.service;

import com.banking.account.dto.AccountResponse;
import com.banking.account.dto.CreateAccountRequest;
import com.banking.account.entity.Account;
import com.banking.account.entity.UpiId;
import com.banking.account.exception.AccountException;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.UpiIdRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final UpiIdRepository upiIdRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        log.info("Creating account for user: {}", userId);

        Account.AccountType accountType;
        try {
            accountType = Account.AccountType.valueOf(request.getAccountType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AccountException("Invalid account type. Use SAVINGS or CURRENT");
        }

        String currency = request.getCurrency() != null ? request.getCurrency().toUpperCase() : "INR";
        if (!List.of("INR", "USD", "EUR", "GBP").contains(currency)) {
            throw new AccountException("Unsupported currency: " + currency);
        }

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .accountType(accountType)
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .accountHolderName(request.getAccountHolderName())
                .build();

        account = accountRepository.save(account);
        log.info("Account created: {} for user: {}", accountNumber, userId);

        // Publish audit event
        kafkaTemplate.send("audit-events",
                String.format("{\"action\":\"ACCOUNT_CREATED\",\"userId\":\"%s\",\"accountId\":\"%s\",\"accountNumber\":\"%s\",\"timestamp\":\"%s\"}",
                        userId, account.getId(), accountNumber, LocalDateTime.now()));

        return mapToResponse(account);
    }

    public List<AccountResponse> getUserAccounts(UUID userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException("Account not found: " + accountId));
        return mapToResponse(account);
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException("Account not found: " + accountNumber));
        return mapToResponse(account);
    }

    public BigDecimal getBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException("Account not found: " + accountId));
        return account.getBalance();
    }

    @Transactional
    public AccountResponse freezeAccount(UUID accountId) {
        log.info("Freezing account: {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException("Account not found: " + accountId));

        if (account.getStatus() == Account.AccountStatus.FROZEN) {
            throw new AccountException("Account is already frozen");
        }

        account.setStatus(Account.AccountStatus.FROZEN);
        account = accountRepository.save(account);

        kafkaTemplate.send("audit-events",
                String.format("{\"action\":\"ACCOUNT_FROZEN\",\"accountId\":\"%s\",\"userId\":\"%s\",\"timestamp\":\"%s\"}",
                        accountId, account.getUserId(), LocalDateTime.now()));

        kafkaTemplate.send("notification-events",
                String.format("{\"type\":\"ACCOUNT_FREEZE\",\"userId\":\"%s\",\"accountId\":\"%s\",\"message\":\"Your account %s has been frozen\",\"timestamp\":\"%s\"}",
                        account.getUserId(), accountId, account.getAccountNumber(), LocalDateTime.now()));

        return mapToResponse(account);
    }

    @Transactional
    public AccountResponse unfreezeAccount(UUID accountId) {
        log.info("Unfreezing account: {}", accountId);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException("Account not found: " + accountId));

        if (account.getStatus() != Account.AccountStatus.FROZEN) {
            throw new AccountException("Account is not frozen");
        }

        account.setStatus(Account.AccountStatus.ACTIVE);
        account = accountRepository.save(account);

        kafkaTemplate.send("audit-events",
                String.format("{\"action\":\"ACCOUNT_UNFROZEN\",\"accountId\":\"%s\",\"userId\":\"%s\",\"timestamp\":\"%s\"}",
                        accountId, account.getUserId(), LocalDateTime.now()));

        return mapToResponse(account);
    }

    @Transactional
    public void updateBalance(UUID accountId, BigDecimal newBalance) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountException("Account not found: " + accountId));
        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    // UPI Methods
    @Transactional
    public String registerUpiId(UUID accountId, String upiIdStr) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException("Account not found: " + accountId));

        if (upiIdRepository.existsByUpiId(upiIdStr)) {
            throw new AccountException("UPI ID already registered: " + upiIdStr);
        }

        if (!upiIdStr.matches("^[a-zA-Z0-9.]+@[a-zA-Z]+$")) {
            throw new AccountException("Invalid UPI ID format. Use format: name@bank");
        }

        UpiId upiId = UpiId.builder()
                .upiId(upiIdStr)
                .account(account)
                .active(true)
                .build();

        upiIdRepository.save(upiId);
        log.info("UPI ID registered: {} for account: {}", upiIdStr, accountId);
        return upiIdStr;
    }

    public AccountResponse resolveUpiId(String upiIdStr) {
        UpiId upiId = upiIdRepository.findByUpiIdAndActiveTrue(upiIdStr)
                .orElseThrow(() -> new AccountException("UPI ID not found or inactive: " + upiIdStr));
        return mapToResponse(upiId.getAccount());
    }

    // Admin methods
    public Page<AccountResponse> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(this::mapToResponse);
    }

    private String generateAccountNumber() {
        Random random = new Random();
        String accountNumber;
        do {
            accountNumber = "10" + String.format("%08d", random.nextInt(100000000));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId().toString())
                .userId(account.getUserId().toString())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().name())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .status(account.getStatus().name())
                .accountHolderName(account.getAccountHolderName())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
