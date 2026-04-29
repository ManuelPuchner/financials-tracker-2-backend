package com.manuelpuchner.backend.transaction.dto;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.transaction.entity.AccountType;
import com.manuelpuchner.backend.transaction.entity.Category;
import com.manuelpuchner.backend.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        @NotNull UUID transactionId,

        @NotNull Instant datetime,
        @NotNull LocalDate date,
        @NotNull AccountType accountType,
        @NotNull Category category,
        @NotNull TransactionType type,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal tax,
        @NotNull String currency,
        String description,

        // Asset fields
        String assetSymbol,
        String assetName,
        AssetClass assetClass,
        BigDecimal shares,
        BigDecimal price,

        // Counterparty fields
        String counterpartyIban,
        String counterpartyName,
        String paymentReference,

        // Card transaction fields
        String merchantName,
        String mccCode,

        // FX fields
        BigDecimal fxOriginalAmount,
        String fxOriginalCurrency,
        BigDecimal fxRate
) {}
