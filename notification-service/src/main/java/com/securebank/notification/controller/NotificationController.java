package com.securebank.notification.controller;

import com.securebank.common.dto.ApiResponse;
import com.securebank.notification.dto.AuditLogResponse;
import com.securebank.notification.dto.NotificationEvent;
import com.securebank.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/log")
    public ResponseEntity<ApiResponse<AuditLogResponse>> logEvent(
            @Valid @RequestBody NotificationEvent event) {
        AuditLogResponse response = notificationService.logEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event logged successfully", response));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsByAccount(
            @PathVariable UUID accountId) {
        List<AuditLogResponse> logs = notificationService.getLogsByAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", logs));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsByUser(
            @PathVariable UUID userId) {
        List<AuditLogResponse> logs = notificationService.getLogsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", logs));
    }
}
