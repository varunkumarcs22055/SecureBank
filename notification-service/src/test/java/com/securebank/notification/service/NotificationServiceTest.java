package com.securebank.notification.service;

import com.securebank.notification.dto.AuditLogResponse;
import com.securebank.notification.dto.NotificationEvent;
import com.securebank.notification.entity.AuditLog;
import com.securebank.notification.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private NotificationService notificationService;

    private AuditLog testLog;
    private UUID testAccountId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testLog = new AuditLog("DEPOSIT", testAccountId, testUserId, "Deposited $1000.00");
        testLog.setId(UUID.randomUUID());
        testLog.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Log event - success")
    void logEvent_ShouldCreateAuditLog() {
        NotificationEvent event = new NotificationEvent("DEPOSIT", testAccountId, testUserId, "Deposited $1000.00");
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testLog);

        AuditLogResponse response = notificationService.logEvent(event);

        assertNotNull(response);
        assertEquals("DEPOSIT", response.getEventType());
        assertEquals(testAccountId, response.getAccountId());
        assertEquals("Deposited $1000.00", response.getMessage());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Get logs by account")
    void getLogsByAccount_ShouldReturnList() {
        when(auditLogRepository.findByAccountIdOrderByCreatedAtDesc(testAccountId))
                .thenReturn(List.of(testLog));

        List<AuditLogResponse> logs = notificationService.getLogsByAccount(testAccountId);

        assertEquals(1, logs.size());
        assertEquals("DEPOSIT", logs.get(0).getEventType());
    }

    @Test
    @DisplayName("Get logs by user")
    void getLogsByUser_ShouldReturnList() {
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(testUserId))
                .thenReturn(List.of(testLog));

        List<AuditLogResponse> logs = notificationService.getLogsByUser(testUserId);

        assertEquals(1, logs.size());
        assertEquals(testUserId, logs.get(0).getUserId());
    }

    @Test
    @DisplayName("Get logs by account - empty list")
    void getLogsByAccount_ShouldReturnEmptyList_WhenNoLogs() {
        UUID unknownAccountId = UUID.randomUUID();
        when(auditLogRepository.findByAccountIdOrderByCreatedAtDesc(unknownAccountId))
                .thenReturn(List.of());

        List<AuditLogResponse> logs = notificationService.getLogsByAccount(unknownAccountId);

        assertTrue(logs.isEmpty());
    }
}
