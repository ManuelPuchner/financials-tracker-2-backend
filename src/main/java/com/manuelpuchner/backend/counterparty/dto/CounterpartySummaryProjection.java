package com.manuelpuchner.backend.counterparty.dto;

import java.math.BigDecimal;

public interface CounterpartySummaryProjection {
    Long getId();
    String getIban();
    String getName();
    BigDecimal getTotalIncome();
    BigDecimal getTotalOutgoing();
    BigDecimal getNet();
    Long getTransactionCount();
}
