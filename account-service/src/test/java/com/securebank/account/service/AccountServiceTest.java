package com.securebank.account.service;

import com.securebank.account.dto.AccountResponse;
import com.securebank.account.dto.CreateAccountRequest;
import com.securebank.account.entity.Account;
import com.securebank.account.entity.AccountStatus;
import com.securebank.account.repository.AccountRepository;
import com.securebank.common.exception.AccountFrozenException;
import com.securebank.common.exception.ResourceNotFoundException;
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
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private UUID testAccountId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testAccount = new Account(testUserId, "SB0000000001");
        testAccount.setId(testAccountId);
        testAccount.setBalance(BigDecimal.valueOf(1000.00));
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Create account - success")
    void createAccount_ShouldReturnNewAccount() {
        CreateAccountRequest request = new CreateAccountRequest(testUserId);

        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountResponse response = accountService.createAccount(request);

        assertNotNull(response);
        assertEquals(testUserId, response.getUserId());
        assertEquals("SB0000000001", response.getAccountNumber());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Get account - success")
    void getAccount_ShouldReturnAccount_WhenExists() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        AccountResponse response = accountService.getAccount(testAccountId);

        assertNotNull(response);
        assertEquals(testAccountId, response.getId());
        assertEquals(BigDecimal.valueOf(1000.00), response.getBalance());
    }

    @Test
    @DisplayName("Get account - not found")
    void getAccount_ShouldThrowException_WhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(accountRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccount(unknownId));
    }

    @Test
    @DisplayName("Get accounts by user")
    void getAccountsByUser_ShouldReturnList() {
        when(accountRepository.findByUserId(testUserId)).thenReturn(List.of(testAccount));

        List<AccountResponse> responses = accountService.getAccountsByUser(testUserId);

        assertEquals(1, responses.size());
        assertEquals(testUserId, responses.get(0).getUserId());
    }

    @Test
    @DisplayName("Freeze account - success")
    void freezeAccount_ShouldSetStatusFrozen() {
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setCreatedAt(testAccount.getCreatedAt());
            return saved;
        });

        AccountResponse response = accountService.freezeAccount(testAccountId);

        assertEquals("FROZEN", response.getStatus());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Unfreeze account - success")
    void unfreezeAccount_ShouldSetStatusActive() {
        testAccount.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setCreatedAt(testAccount.getCreatedAt());
            return saved;
        });

        AccountResponse response = accountService.unfreezeAccount(testAccountId);

        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    @DisplayName("Unfreeze account - not frozen throws exception")
    void unfreezeAccount_ShouldThrowException_WhenNotFrozen() {
        testAccount.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        assertThrows(AccountFrozenException.class, () -> accountService.unfreezeAccount(testAccountId));
    }
}
