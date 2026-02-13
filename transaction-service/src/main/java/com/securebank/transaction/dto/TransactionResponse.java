package com.securebank.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionResponse {

    private UUID id;
    private UUID accountId;
    private UUID targetAccountId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt;

    public TransactionResponse() {}

    public TransactionResponse(UUID id, UUID accountId, UUID targetAccountId, String type,
                                BigDecimal amount, BigDecimal balanceAfter, String description,
                                LocalDateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.targetAccountId = targetAccountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getTargetAccountId() { return targetAccountId; }
    public void setTargetAccountId(UUID targetAccountId) { this.targetAccountId = targetAccountId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
