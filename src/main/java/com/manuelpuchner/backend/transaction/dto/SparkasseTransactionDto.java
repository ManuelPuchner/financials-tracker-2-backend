package com.manuelpuchner.backend.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.OffsetDateTime;

/**
 * Represents a single transaction from the Sparkasse JSON export.
 * Unknown fields are ignored to stay forward-compatible with API changes.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkasseTransactionDto {

    // Sparkasse's own transaction IDs — null in almost all real exports
    private String transactionId;
    private String containedTransactionId;

    // Booking date (the date that matters for display)
    private OffsetDateTime booking;
    private OffsetDateTime valuation;

    private String partnerName;
    private SparkasseAccountDto partnerAccount;

    private SparkasseAmountDto amount;

    // Payment reference / description
    private String reference;

    // Stable unique identifier — use for deterministic UUID when present
    private String referenceNumber;

    private String note;

    private String sepaMandateId;
    private String sepaCreditorId;

    private String ownerAccountNumber;
    private String ownerAccountTitle;

    // Card-specific fields — set for card/virtual card transactions
    private String virtualCardNumber;
    private String virtualCardDeviceName;
    private String virtualCardMobilePaymentApplicationName;
    private String cardNumber;
    private String cardType;
    private String cardBrand;

    // Explicit merchant name (may be null even for card transactions)
    private String merchantName;

    private String paymentMethod;

    private String receiverReference;

    // -----------------------------------------------------------------------

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SparkasseAccountDto {
        private String iban;
        private String bic;
        private String number;
        private String bankCode;
        private String countryCode;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SparkasseAmountDto {
        /** Integer value scaled by precision. E.g. value=-2606, precision=2 → -26.06 */
        private Long value;
        private Integer precision;
        private String currency;

        public BigDecimal toBigDecimal() {
            if (value == null) return null;
            int scale = precision != null ? precision : 2;
            return BigDecimal.valueOf(value).movePointLeft(scale);
        }
    }
}
