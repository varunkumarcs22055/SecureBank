package com.securebank.transaction.service;

import com.securebank.common.exception.AccountFrozenException;
import com.securebank.common.exception.BadRequestException;
import com.securebank.common.exception.InsufficientBalanceException;
import com.securebank.common.exception.ResourceNotFoundException;
import com.securebank.transaction.dto.*;
import com.securebank.transaction.entity.Account;
import com.securebank.transaction.entity.Transaction;
import com.securebank.transaction.entity.TransactionType;
import com.securebank.transaction.repository.AccountRepository;
import com.securebank.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        log.info("Processing deposit of {} to account {}", request.getAmount(), request.getAccountId());

        Account account = accountRepository.findByIdWithLock(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountId()));

        validateAccountActive(account);

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                account.getId(), null, TransactionType.DEPOSIT,
                request.getAmount(), newBalance,
                request.getDescription() != null ? request.getDescription() : "Deposit"
        );
        Transaction savedTxn = transactionRepository.save(transaction);

        log.info("Deposit completed. Account: {}, New Balance: {}", account.getId(), newBalance);
        return mapToResponse(savedTxn);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        log.info("Processing withdrawal of {} from account {}", request.getAmount(), request.getAccountId());

        Account account = accountRepository.findByIdWithLock(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountId()));

        validateAccountActive(account);
        validateSufficientBalance(account, request.getAmount());

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                account.getId(), null, TransactionType.WITHDRAWAL,
                request.getAmount(), newBalance,
                request.getDescription() != null ? request.getDescription() : "Withdrawal"
        );
        Transaction savedTxn = transactionRepository.save(transaction);

        log.info("Withdrawal completed. Account: {}, New Balance: {}", account.getId(), newBalance);
        return mapToResponse(savedTxn);
    }

    /**
     * Concurrency-safe transfer using pessimistic locking.
     * Accounts are locked in UUID natural order to prevent deadlocks.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransactionResponse transfer(TransferRequest request) {
        log.info("Processing transfer of {} from {} to {}",
                request.getAmount(), request.getFromAccountId(), request.getToAccountId());

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new BadRequestException("Cannot transfer to the same account");
        }

        // Lock accounts in deterministic order (by UUID) to prevent deadlock
        UUID firstId, secondId;
        if (request.getFromAccountId().compareTo(request.getToAccountId()) < 0) {
            firstId = request.getFromAccountId();
            secondId = request.getToAccountId();
        } else {
            firstId = request.getToAccountId();
            secondId = request.getFromAccountId();
        }

        Account firstAccount = accountRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + firstId));
        Account secondAccount = accountRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + secondId));

        // Map back to from/to
        Account fromAccount = firstId.equals(request.getFromAccountId()) ? firstAccount : secondAccount;
        Account toAccount = firstId.equals(request.getToAccountId()) ? firstAccount : secondAccount;

        validateAccountActive(fromAccount);
        validateAccountActive(toAccount);
        validateSufficientBalance(fromAccount, request.getAmount());

        // Debit source
        BigDecimal fromNewBalance = fromAccount.getBalance().subtract(request.getAmount());
        fromAccount.setBalance(fromNewBalance);
        accountRepository.save(fromAccount);

        // Credit target
        BigDecimal toNewBalance = toAccount.getBalance().add(request.getAmount());
        toAccount.setBalance(toNewBalance);
        accountRepository.save(toAccount);

        // Record debit transaction
        String description = request.getDescription() != null ? request.getDescription()
                : "Transfer to " + toAccount.getAccountNumber();
        Transaction debitTxn = new Transaction(
                fromAccount.getId(), toAccount.getId(), TransactionType.TRANSFER,
                request.getAmount(), fromNewBalance, description
        );
        Transaction savedDebitTxn = transactionRepository.save(debitTxn);

        // Record credit transaction
        Transaction creditTxn = new Transaction(
                toAccount.getId(), fromAccount.getId(), TransactionType.TRANSFER,
                request.getAmount(), toNewBalance,
                "Transfer from " + fromAccount.getAccountNumber()
        );
        transactionRepository.save(creditTxn);

        log.info("Transfer completed. From: {} (balance: {}), To: {} (balance: {})",
                fromAccount.getId(), fromNewBalance, toAccount.getId(), toNewBalance);

        return mapToResponse(savedDebitTxn);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(UUID accountId) {
        log.debug("Fetching transaction history for account: {}", accountId);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateAccountActive(Account account) {
        if ("FROZEN".equals(account.getStatus())) {
            throw new AccountFrozenException("Account is frozen: " + account.getId());
        }
    }

    private void validateSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + account.getBalance() + ", Requested: " + amount);
        }
    }

    private TransactionResponse mapToResponse(Transaction txn) {
        return new TransactionResponse(
                txn.getId(), txn.getAccountId(), txn.getTargetAccountId(),
                txn.getType().name(), txn.getAmount(), txn.getBalanceAfter(),
                txn.getDescription(), txn.getCreatedAt()
        );
    }
}
