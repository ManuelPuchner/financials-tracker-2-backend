package com.manuelpuchner.backend.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AssetPositionDto(
        String symbol,
        String name,
        BigDecimal totalShares,
        BigDecimal totalInvested,
        BigDecimal totalDividendsReceived
) {}
