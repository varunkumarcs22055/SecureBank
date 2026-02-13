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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account sourceAccount;
    private Account targetAccount;
    private UUID sourceAccountId;
    private UUID targetAccountId;

    @BeforeEach
    void setUp() {
        sourceAccountId = UUID.randomUUID();
        targetAccountId = UUID.randomUUID();

        sourceAccount = new Account();
        sourceAccount.setId(sourceAccountId);
        sourceAccount.setAccountNumber("SB0000000001");
        sourceAccount.setBalance(BigDecimal.valueOf(5000.00));
        sourceAccount.setStatus("ACTIVE");
        sourceAccount.setUserId(UUID.randomUUID());

        targetAccount = new Account();
        targetAccount.setId(targetAccountId);
        targetAccount.setAccountNumber("SB0000000002");
        targetAccount.setBalance(BigDecimal.valueOf(3000.00));
        targetAccount.setStatus("ACTIVE");
        targetAccount.setUserId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Deposit - success")
    void deposit_ShouldIncreaseBalance() {
        DepositRequest request = new DepositRequest(sourceAccountId, BigDecimal.valueOf(1000.00), "Salary");

        when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sourceAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(UUID.randomUUID());
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.deposit(request);

        assertNotNull(response);
        assertEquals("DEPOSIT", response.getType());
        assertEquals(BigDecimal.valueOf(6000.00), response.getBalanceAfter());
    }

    @Test
    @DisplayName("Deposit - account not found")
    void deposit_ShouldThrowException_WhenAccountNotFound() {
        DepositRequest request = new DepositRequest(sourceAccountId, BigDecimal.valueOf(100.00), null);
        when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.deposit(request));
    }

    @Test
    @DisplayName("Deposit - frozen account throws exception")
    void deposit_ShouldThrowException_WhenAccountFrozen() {
        sourceAccount.setStatus("FROZEN");
        DepositRequest request = new DepositRequest(sourceAccountId, BigDecimal.valueOf(100.00), null);
        when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));

        assertThrows(AccountFrozenException.class, () -> transactionService.deposit(request));
    }

    @Test
    @DisplayName("Withdraw - success")
    void withdraw_ShouldDecreaseBalance() {
        WithdrawRequest request = new WithdrawRequest(sourceAccountId, BigDecimal.valueOf(1000.00), "ATM");

        when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sourceAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(UUID.randomUUID());
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.withdraw(request);

        assertNotNull(response);
        assertEquals("WITHDRAWAL", response.getType());
        assertEquals(BigDecimal.valueOf(4000.00), response.getBalanceAfter());
    }

    @Test
    @DisplayName("Withdraw - insufficient balance")
    void withdraw_ShouldThrowException_WhenInsufficientBalance() {
        WithdrawRequest request = new WithdrawRequest(sourceAccountId, BigDecimal.valueOf(10000.00), null);
        when(accountRepository.findByIdWithLock(sourceAccountId)).thenReturn(Optional.of(sourceAccount));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(request));
    }

    @Test
    @DisplayName("Transfer - success")
    void transfer_ShouldDebitSourceAndCreditTarget() {
        TransferRequest request = new TransferRequest(
                sourceAccountId, targetAccountId, BigDecimal.valueOf(2000.00), "Rent payment");

        // Determine lock order
        UUID firstId = sourceAccountId.compareTo(targetAccountId) < 0 ? sourceAccountId : targetAccountId;
        UUID secondId = firstId.equals(sourceAccountId) ? targetAccountId : sourceAccountId;
        Account firstAccount = firstId.equals(sourceAccountId) ? sourceAccount : targetAccount;
        Account secondAccount = firstId.equals(sourceAccountId) ? targetAccount : sourceAccount;

        when(accountRepository.findByIdWithLock(firstId)).thenReturn(Optional.of(firstAccount));
        when(accountRepository.findByIdWithLock(secondId)).thenReturn(Optional.of(secondAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            txn.setId(UUID.randomUUID());
            txn.setCreatedAt(LocalDateTime.now());
            return txn;
        });

        TransactionResponse response = transactionService.transfer(request);

        assertNotNull(response);
        assertEquals("TRANSFER", response.getType());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Transfer - same account throws exception")
    void transfer_ShouldThrowException_WhenSameAccount() {
        TransferRequest request = new TransferRequest(
                sourceAccountId, sourceAccountId, BigDecimal.valueOf(100.00), null);

        assertThrows(BadRequestException.class, () -> transactionService.transfer(request));
    }

    @Test
    @DisplayName("Transfer - insufficient balance")
    void transfer_ShouldThrowException_WhenInsufficientBalance() {
        TransferRequest request = new TransferRequest(
                sourceAccountId, targetAccountId, BigDecimal.valueOf(50000.00), null);

        UUID firstId = sourceAccountId.compareTo(targetAccountId) < 0 ? sourceAccountId : targetAccountId;
        UUID secondId = firstId.equals(sourceAccountId) ? targetAccountId : sourceAccountId;
        Account firstAccount = firstId.equals(sourceAccountId) ? sourceAccount : targetAccount;
        Account secondAccount = firstId.equals(sourceAccountId) ? targetAccount : sourceAccount;

        when(accountRepository.findByIdWithLock(firstId)).thenReturn(Optional.of(firstAccount));
        when(accountRepository.findByIdWithLock(secondId)).thenReturn(Optional.of(secondAccount));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.transfer(request));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Transfer - frozen source account throws exception")
    void transfer_ShouldThrowException_WhenSourceFrozen() {
        sourceAccount.setStatus("FROZEN");
        TransferRequest request = new TransferRequest(
                sourceAccountId, targetAccountId, BigDecimal.valueOf(100.00), null);

        UUID firstId = sourceAccountId.compareTo(targetAccountId) < 0 ? sourceAccountId : targetAccountId;
        UUID secondId = firstId.equals(sourceAccountId) ? targetAccountId : sourceAccountId;
        Account firstAccount = firstId.equals(sourceAccountId) ? sourceAccount : targetAccount;
        Account secondAccount = firstId.equals(sourceAccountId) ? targetAccount : sourceAccount;

        when(accountRepository.findByIdWithLock(firstId)).thenReturn(Optional.of(firstAccount));
        when(accountRepository.findByIdWithLock(secondId)).thenReturn(Optional.of(secondAccount));

        assertThrows(AccountFrozenException.class, () -> transactionService.transfer(request));
    }

    @Test
    @DisplayName("Get transaction history")
    void getTransactionHistory_ShouldReturnList() {
        Transaction txn = new Transaction(sourceAccountId, null, TransactionType.DEPOSIT,
                BigDecimal.valueOf(500.00), BigDecimal.valueOf(5500.00), "Test deposit");
        txn.setId(UUID.randomUUID());
        txn.setCreatedAt(LocalDateTime.now());

        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(sourceAccountId))
                .thenReturn(List.of(txn));

        List<TransactionResponse> history = transactionService.getTransactionHistory(sourceAccountId);

        assertEquals(1, history.size());
        assertEquals("DEPOSIT", history.get(0).getType());
    }
}
