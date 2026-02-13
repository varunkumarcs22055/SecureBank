package com.securebank.notification.repository;

import com.securebank.notification.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
