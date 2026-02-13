package com.securebank.account.service;

import com.securebank.account.dto.AccountResponse;
import com.securebank.account.dto.CreateAccountRequest;
import com.securebank.account.entity.Account;
import com.securebank.account.entity.AccountStatus;
import com.securebank.account.repository.AccountRepository;
import com.securebank.common.exception.AccountFrozenException;
import com.securebank.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for user: {}", request.getUserId());

        String accountNumber = generateAccountNumber();
        Account account = new Account(request.getUserId(), accountNumber);
        Account savedAccount = accountRepository.save(account);

        log.info("Account created: {} for user: {}", accountNumber, request.getUserId());
        return mapToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        log.debug("Fetching account: {}", accountId);
        Account account = findAccountById(accountId);
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(UUID userId) {
        log.debug("Fetching accounts for user: {}", userId);
        return accountRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse freezeAccount(UUID accountId) {
        log.info("Freezing account: {}", accountId);
        Account account = findAccountById(accountId);
        account.setStatus(AccountStatus.FROZEN);
        Account updatedAccount = accountRepository.save(account);
        log.info("Account frozen: {}", accountId);
        return mapToResponse(updatedAccount);
    }

    @Transactional
    public AccountResponse unfreezeAccount(UUID accountId) {
        log.info("Unfreezing account: {}", accountId);
        Account account = findAccountById(accountId);

        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new AccountFrozenException("Account is not frozen: " + accountId);
        }

        account.setStatus(AccountStatus.ACTIVE);
        Account updatedAccount = accountRepository.save(account);
        log.info("Account unfrozen: {}", accountId);
        return mapToResponse(updatedAccount);
    }

    private Account findAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
    }

    private String generateAccountNumber() {
        Random random = new Random();
        String accountNumber;
        do {
            accountNumber = String.format("SB%010d", Math.abs(random.nextLong() % 10000000000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}
