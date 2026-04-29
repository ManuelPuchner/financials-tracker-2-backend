package com.manuelpuchner.backend.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OverviewResponse(
        BigDecimal cashBalance,
        String currency,
        long totalTransactionCount,
        BigDecimal totalFeesAndTaxesPaid
) {}
