package com.manuelpuchner.backend.dashboard.service;

import com.manuelpuchner.backend.account.dto.AccountResponse;
import com.manuelpuchner.backend.account.entity.Account;
import com.manuelpuchner.backend.account.repository.AccountRepository;
import com.manuelpuchner.backend.dashboard.dto.*;
import com.manuelpuchner.backend.transaction.entity.TransactionType;
import com.manuelpuchner.backend.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public OverviewResponse overview(Long accountId) {
        validateAccountId(accountId);
        BigDecimal amounts = orZero(transactionRepository.sumAllAmounts(accountId));
        BigDecimal feesAndTaxes = transactionRepository.sumFeesAndTaxes(accountId);
        BigDecimal cashBalance = amounts.add(feesAndTaxes);
        long count = accountId != null
                ? transactionRepository.countByAccount_Id(accountId)
                : transactionRepository.count();

        return OverviewResponse.builder()
                .cashBalance(cashBalance)
                .currency("EUR")
                .totalTransactionCount(count)
                .totalFeesAndTaxesPaid(feesAndTaxes.negate())
                .build();
    }

    @Transactional(readOnly = true)
    public PortfolioResponse portfolio(Long accountId) {
        validateAccountId(accountId);
        List<AssetPositionDto> positions = transactionRepository.findAssetPositions(accountId);

        BigDecimal totalInvested = positions.stream()
                .map(AssetPositionDto::totalInvested)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDividends = positions.stream()
                .map(AssetPositionDto::totalDividendsReceived)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PortfolioResponse.builder()
                .totalInvested(totalInvested)
                .totalDividendsReceived(totalDividends)
                .positions(positions)
                .build();
    }

    @Transactional(readOnly = true)
    public SpendingResponse spending(Long accountId) {
        validateAccountId(accountId);
        BigDecimal cardSpend = orZero(transactionRepository.sumAmountByTypes(
                Set.of(TransactionType.CARD_TRANSACTION, TransactionType.CARD_TRANSACTION_INTERNATIONAL), accountId)).negate();
        BigDecimal transferOut = orZero(transactionRepository.sumAmountByTypes(
                Set.of(TransactionType.TRANSFER_INSTANT_OUTBOUND), accountId)).negate();
        List<TopMerchantDto> topMerchants = transactionRepository.findTopMerchants(accountId, PageRequest.of(0, 10));

        return SpendingResponse.builder()
                .totalCardSpending(cardSpend)
                .totalTransferOutbound(transferOut)
                .topMerchants(topMerchants)
                .build();
    }

    @Transactional(readOnly = true)
    public IncomeResponse income(Long accountId) {
        validateAccountId(accountId);
        BigDecimal interest = transactionRepository.sumAmountByTypes(Set.of(TransactionType.INTEREST_PAYMENT), accountId);
        BigDecimal dividends = transactionRepository.sumAmountByTypes(Set.of(TransactionType.DIVIDEND), accountId);
        BigDecimal bonuses = transactionRepository.sumAmountByTypes(Set.of(TransactionType.BONUS), accountId);
        BigDecimal saveback = transactionRepository.sumAmountByTypes(Set.of(TransactionType.BENEFITS_SAVEBACK), accountId);
        BigDecimal transferIn = transactionRepository.sumAmountByTypes(
                Set.of(TransactionType.TRANSFER_INBOUND, TransactionType.TRANSFER_INSTANT_INBOUND), accountId);
        BigDecimal customerIn = transactionRepository.sumAmountByTypes(
                Set.of(TransactionType.CUSTOMER_INPAYMENT, TransactionType.CUSTOMER_INBOUND), accountId);

        BigDecimal total = Stream.of(interest, dividends, bonuses, saveback, transferIn, customerIn).map(this::orZero).reduce(BigDecimal.ZERO, BigDecimal::add);

        return IncomeResponse.builder()
                .totalIncome(total)
                .interestPayments(orZero(interest))
                .dividends(orZero(dividends))
                .bonuses(orZero(bonuses))
                .saveback(orZero(saveback))
                .transferInbound(orZero(transferIn))
                .customerInpayments(orZero(customerIn))
                .build();
    }

    @Transactional(readOnly = true)
    public List<AccountDashboardDto> byAccount() {
        return accountRepository.findAll().stream()
                .map(a -> AccountDashboardDto.builder()
                        .account(toAccountResponse(a))
                        .overview(overview(a.getId()))
                        .portfolio(portfolio(a.getId()))
                        .spending(spending(a.getId()))
                        .income(income(a.getId()))
                        .build())
                .toList();
    }

    private AccountResponse toAccountResponse(Account a) {
        return AccountResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .color(a.getColor())
                .icon(a.getIcon())
                .source(a.getSource())
                .ownAccountIban(a.getOwnAccountIban())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private void validateAccountId(Long accountId) {
        if (accountId != null && !accountRepository.existsById(accountId)) {
            throw new EntityNotFoundException("Account not found: " + accountId);
        }
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
