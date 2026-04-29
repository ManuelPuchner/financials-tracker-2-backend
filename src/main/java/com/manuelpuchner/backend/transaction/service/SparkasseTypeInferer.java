package com.manuelpuchner.backend.transaction.service;

import com.manuelpuchner.backend.transaction.dto.SparkasseTransactionDto;
import com.manuelpuchner.backend.transaction.entity.TransactionType;

import java.math.BigDecimal;

/**
 * Infers TransactionType from a Sparkasse JSON transaction.
 *
 * Card detection uses JSON fields that are richer than what the CSV provides:
 *   - virtualCardNumber / cardNumber / cardBrand are set for card payments
 *   - merchantName is set when Sparkasse can identify the merchant
 *
 * All other debits (SEPA direct debits, bank transfers, fees) → TRANSFER_INSTANT_OUTBOUND.
 * All credits → TRANSFER_INBOUND.
 */
public final class SparkasseTypeInferer {

    private SparkasseTypeInferer() {}

    public static TransactionType infer(SparkasseTransactionDto tx, BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            throw new IllegalArgumentException("Zero or null amount rows must be skipped before type inference");
        }

        if (amount.signum() < 0) {
            if (isCardTransaction(tx)) {
                return TransactionType.CARD_TRANSACTION;
            }
            return TransactionType.TRANSFER_INSTANT_OUTBOUND;
        }

        return TransactionType.TRANSFER_INBOUND;
    }

    public static boolean isCardTransaction(SparkasseTransactionDto tx) {
        // Any card-related field being set is a reliable indicator of a card payment
        return tx.getVirtualCardNumber() != null
                || tx.getCardNumber() != null
                || tx.getCardBrand() != null
                || tx.getCardType() != null
                || tx.getMerchantName() != null;
    }
}
