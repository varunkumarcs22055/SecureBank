package com.securebank.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class AuditLogResponse {

    private UUID id;
    private String eventType;
    private UUID accountId;
    private UUID userId;
    private String message;
    private LocalDateTime createdAt;

    public AuditLogResponse() {}

    public AuditLogResponse(UUID id, String eventType, UUID accountId, UUID userId,
                            String message, LocalDateTime createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.accountId = accountId;
        this.userId = userId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
