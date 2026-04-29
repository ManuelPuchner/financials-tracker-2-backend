package com.manuelpuchner.backend.transaction.dto;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.transaction.entity.AccountType;
import com.manuelpuchner.backend.transaction.entity.Category;
import com.manuelpuchner.backend.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CsvRow(
        Instant datetime,
        LocalDate date,
        AccountType accountType,
        Category category,
        TransactionType type,
        AssetClass assetClass,
        String assetName,
        String assetSymbol,
        BigDecimal shares,
        BigDecimal price,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal tax,
        String currency,
        BigDecimal fxOriginalAmount,
        String fxOriginalCurrency,
        BigDecimal fxRate,
        String description,
        UUID transactionId,
        String counterpartyName,
        String counterpartyIban,
        String paymentReference,
        String mccCode,
        String merchantName
) {}
