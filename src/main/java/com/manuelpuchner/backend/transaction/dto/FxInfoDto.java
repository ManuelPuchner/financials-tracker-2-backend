package com.manuelpuchner.backend.transaction.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record FxInfoDto(
        BigDecimal originalAmount,
        String originalCurrency,
        BigDecimal fxRate
) {}
