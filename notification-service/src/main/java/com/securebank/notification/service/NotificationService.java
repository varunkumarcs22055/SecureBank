package com.securebank.notification.service;

import com.securebank.notification.dto.AuditLogResponse;
import com.securebank.notification.dto.NotificationEvent;
import com.securebank.notification.entity.AuditLog;
import com.securebank.notification.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AuditLogRepository auditLogRepository;

    public NotificationService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLogResponse logEvent(NotificationEvent event) {
        log.info("Logging event: {} for account: {}", event.getEventType(), event.getAccountId());

        AuditLog auditLog = new AuditLog(
                event.getEventType(),
                event.getAccountId(),
                event.getUserId(),
                event.getMessage()
        );

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log created: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByAccount(UUID accountId) {
        log.debug("Fetching audit logs for account: {}", accountId);
        return auditLogRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByUser(UUID userId) {
        log.debug("Fetching audit logs for user: {}", userId);
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEventType(),
                auditLog.getAccountId(),
                auditLog.getUserId(),
                auditLog.getMessage(),
                auditLog.getCreatedAt()
        );
    }
}
