package com.manuelpuchner.backend.dashboard.controller;

import com.manuelpuchner.backend.dashboard.dto.AccountDashboardDto;
import com.manuelpuchner.backend.dashboard.dto.IncomeResponse;
import com.manuelpuchner.backend.dashboard.dto.OverviewResponse;
import com.manuelpuchner.backend.dashboard.dto.PortfolioResponse;
import com.manuelpuchner.backend.dashboard.dto.SpendingResponse;
import com.manuelpuchner.backend.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public OverviewResponse overview(@RequestParam(required = false) Long accountId) {
        return dashboardService.overview(accountId);
    }

    @GetMapping("/portfolio")
    public PortfolioResponse portfolio(@RequestParam(required = false) Long accountId) {
        return dashboardService.portfolio(accountId);
    }

    @GetMapping("/spending")
    public SpendingResponse spending(@RequestParam(required = false) Long accountId) {
        return dashboardService.spending(accountId);
    }

    @GetMapping("/income")
    public IncomeResponse income(@RequestParam(required = false) Long accountId) {
        return dashboardService.income(accountId);
    }

    @GetMapping("/by-account")
    public List<AccountDashboardDto> byAccount() {
        return dashboardService.byAccount();
    }
}
