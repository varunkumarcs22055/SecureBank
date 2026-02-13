package com.securebank.notification.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class NotificationEvent {

    @NotBlank(message = "Event type is required")
    private String eventType;

    private UUID accountId;
    private UUID userId;
    private String message;

    public NotificationEvent() {}

    public NotificationEvent(String eventType, UUID accountId, UUID userId, String message) {
        this.eventType = eventType;
        this.accountId = accountId;
        this.userId = userId;
        this.message = message;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
