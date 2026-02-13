package com.securebank.account.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateAccountRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    public CreateAccountRequest() {}

    public CreateAccountRequest(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
