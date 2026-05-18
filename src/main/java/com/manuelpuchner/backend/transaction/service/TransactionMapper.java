package com.manuelpuchner.backend.transaction.service;

import com.manuelpuchner.backend.account.entity.Account;
import com.manuelpuchner.backend.asset.entity.Asset;
import com.manuelpuchner.backend.counterparty.entity.Counterparty;
import com.manuelpuchner.backend.mcc.entity.MccCode;
import com.manuelpuchner.backend.transaction.dto.*;
import com.manuelpuchner.backend.transaction.entity.FxInfo;
import com.manuelpuchner.backend.transaction.entity.Transaction;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class TransactionMapper {

    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");

    public TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .transactionId(t.getTransactionId())
                .source(t.getSource())
                .datetime(t.getDatetime() != null ? OffsetDateTime.ofInstant(t.getDatetime(), VIENNA) : null)
                .date(t.getDate())
                .accountType(t.getAccountType())
                .category(t.getCategory())
                .type(t.getType())
                .amount(t.getAmount())
                .fee(t.getFee())
                .tax(t.getTax())
                .currency(t.getCurrency())
                .description(t.getDescription())
                .note(t.getNote())
                .assetInfo(toAssetInfoDto(t))
                .fxInfo(toFxInfoDto(t.getFxInfo()))
                .counterpartyInfo(toCounterpartyInfoDto(t))
                .merchantName(t.getMerchantName())
                .rawMerchantName(t.getRawMerchantName())
                .mccCode(toMccCodeDto(t.getMccCode()))
                .userCategory(toUserCategoryResponse(t.getUserCategory()))
                .account(toAccountSummaryDto(t.getAccount()))
                .ownAccountIban(t.getOwnAccountIban())
                .ownAccountName(t.getOwnAccountName())
                .sepaMandateId(t.getSepaMandateId())
                .sepaCreditorId(t.getSepaCreditorId())
                .paymentMethod(t.getPaymentMethod())
                .receiverReference(t.getReceiverReference())
                .build();
    }

    private AssetInfoDto toAssetInfoDto(Transaction t) {
        Asset a = t.getAsset();
        if (a == null) return null;
        return AssetInfoDto.builder()
                .assetId(a.getId())
                .symbol(a.getSymbol())
                .name(a.getName())
                .assetClass(a.getAssetClass())
                .shares(t.getShares())
                .price(t.getPrice())
                .build();
    }

    private FxInfoDto toFxInfoDto(FxInfo f) {
        if (f == null || f.getOriginalCurrency() == null || f.getOriginalCurrency().isBlank()) return null;
        return FxInfoDto.builder()
                .originalAmount(f.getOriginalAmount())
                .originalCurrency(f.getOriginalCurrency())
                .fxRate(f.getFxRate())
                .build();
    }

    private CounterpartyInfoDto toCounterpartyInfoDto(Transaction t) {
        Counterparty c = t.getCounterparty();
        if (c == null) return null;
        return CounterpartyInfoDto.builder()
                .counterpartyId(c.getId())
                .iban(c.getIban())
                .name(c.getName())
                .paymentReference(t.getPaymentReference())
                .build();
    }

    private MccCodeDto toMccCodeDto(MccCode m) {
        if (m == null) return null;
        UserCategory uc = m.getUserCategory();
        return MccCodeDto.builder()
                .mcc(m.getMcc())
                .description(m.getCombinedDescription() != null ? m.getCombinedDescription() : m.getEditedDescription())
                .userCategory(uc != null ? UserCategoryResponse.builder()
                        .id(uc.getId()).name(uc.getName()).color(uc.getColor()).icon(uc.getIcon()).build() : null)
                .build();
    }

    private AccountSummaryDto toAccountSummaryDto(Account a) {
        if (a == null) return null;
        return AccountSummaryDto.builder()
                .id(a.getId())
                .name(a.getName())
                .color(a.getColor())
                .icon(a.getIcon())
                .source(a.getSource())
                .ownAccountIban(a.getOwnAccountIban())
                .build();
    }

    private UserCategoryResponse toUserCategoryResponse(UserCategory c) {
        if (c == null) return null;
        return UserCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .color(c.getColor())
                .icon(c.getIcon())
                .build();
    }
}
