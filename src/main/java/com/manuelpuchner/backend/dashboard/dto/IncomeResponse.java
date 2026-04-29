package com.manuelpuchner.backend.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record IncomeResponse(
        BigDecimal totalIncome,
        BigDecimal interestPayments,
        BigDecimal dividends,
        BigDecimal bonuses,
        BigDecimal saveback,
        BigDecimal transferInbound,
        BigDecimal customerInpayments
) {}
