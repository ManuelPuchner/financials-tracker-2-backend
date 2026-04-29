package com.manuelpuchner.backend.transaction.dto;

import com.manuelpuchner.backend.transaction.entity.TransactionSource;
import lombok.Builder;

@Builder
public record AccountSummaryDto(
        Long id,
        String name,
        String color,
        String icon,
        TransactionSource source,
        String ownAccountIban
) {}
