package com.manuelpuchner.backend.transaction.dto;

import com.manuelpuchner.backend.transaction.entity.AccountType;
import com.manuelpuchner.backend.transaction.entity.Category;
import com.manuelpuchner.backend.transaction.entity.TransactionSource;
import com.manuelpuchner.backend.transaction.entity.TransactionType;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record TransactionResponse(
        Long id,
        UUID transactionId,
        TransactionSource source,
        OffsetDateTime datetime,
        LocalDate date,
        AccountType accountType,
        Category category,
        TransactionType type,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal tax,
        String currency,
        String description,
        String note,
        AssetInfoDto assetInfo,
        FxInfoDto fxInfo,
        CounterpartyInfoDto counterpartyInfo,
        String merchantName,
        String rawMerchantName,
        MccCodeDto mccCode,
        UserCategoryResponse userCategory,
        AccountSummaryDto account,
        String ownAccountIban,
        String ownAccountName,
        String sepaMandateId,
        String sepaCreditorId,
        String paymentMethod,
        String receiverReference
) {}
