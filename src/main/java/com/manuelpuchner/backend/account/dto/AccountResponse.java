package com.manuelpuchner.backend.account.dto;

import com.manuelpuchner.backend.transaction.entity.TransactionSource;
import lombok.Builder;

import java.time.Instant;

@Builder
public record AccountResponse(
        Long id,
        String name,
        String color,
        String icon,
        TransactionSource source,
        String ownAccountIban,
        Instant createdAt,
        Instant updatedAt
) {}
