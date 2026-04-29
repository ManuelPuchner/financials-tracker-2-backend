package com.manuelpuchner.backend.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxInfo {

    @Column(name = "fx_original_amount", precision = 20, scale = 6)
    private BigDecimal originalAmount;

    @Column(name = "fx_original_currency", length = 3)
    private String originalCurrency;

    @Column(name = "fx_rate", precision = 20, scale = 10)
    private BigDecimal fxRate;
}
