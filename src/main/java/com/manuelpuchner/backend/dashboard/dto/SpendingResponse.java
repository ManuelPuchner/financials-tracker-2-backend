package com.manuelpuchner.backend.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record SpendingResponse(
        BigDecimal totalCardSpending,
        BigDecimal totalTransferOutbound,
        List<TopMerchantDto> topMerchants
) {}
