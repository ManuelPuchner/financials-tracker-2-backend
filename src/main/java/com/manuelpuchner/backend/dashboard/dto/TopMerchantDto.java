package com.manuelpuchner.backend.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TopMerchantDto(
        String merchantName,
        BigDecimal totalSpent,
        long transactionCount
) {}
