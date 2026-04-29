package com.manuelpuchner.backend.dashboard.dto;

import com.manuelpuchner.backend.account.dto.AccountResponse;
import lombok.Builder;

@Builder
public record AccountDashboardDto(
        AccountResponse account,
        OverviewResponse overview,
        PortfolioResponse portfolio,
        SpendingResponse spending,
        IncomeResponse income
) {}
