package com.manuelpuchner.backend.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PortfolioResponse(
        BigDecimal totalInvested,
        BigDecimal totalDividendsReceived,
        List<AssetPositionDto> positions
) {}
