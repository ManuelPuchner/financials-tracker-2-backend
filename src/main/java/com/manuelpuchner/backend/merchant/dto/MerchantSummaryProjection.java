package com.manuelpuchner.backend.merchant.dto;

import java.math.BigDecimal;

public interface MerchantSummaryProjection {
    String getName();
    BigDecimal getTotalIncome();
    BigDecimal getTotalOutgoing();
    BigDecimal getNet();
    Long getTransactionCount();
}
