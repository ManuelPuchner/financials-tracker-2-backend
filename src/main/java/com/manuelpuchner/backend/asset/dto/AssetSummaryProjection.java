package com.manuelpuchner.backend.asset.dto;

import com.manuelpuchner.backend.asset.entity.AssetClass;

import java.math.BigDecimal;

public interface AssetSummaryProjection {
    Long getId();
    String getSymbol();
    String getName();
    AssetClass getAssetClass();
    BigDecimal getTotalIncome();
    BigDecimal getTotalOutgoing();
    BigDecimal getNet();
    Long getTransactionCount();
}
