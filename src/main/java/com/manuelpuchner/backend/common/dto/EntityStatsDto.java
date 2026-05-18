package com.manuelpuchner.backend.common.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record EntityStatsDto(
        BigDecimal totalIncome,
        BigDecimal totalOutgoing,
        BigDecimal net,
        long transactionCount
) {}
