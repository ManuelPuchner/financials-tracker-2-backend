package com.manuelpuchner.backend.transaction.entity;

import com.manuelpuchner.backend.account.entity.Account;
import com.manuelpuchner.backend.asset.entity.Asset;
import com.manuelpuchner.backend.counterparty.entity.Counterparty;
import com.manuelpuchner.backend.mcc.entity.MccCode;
import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, updatable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionSource source;

    @Column(name = "transaction_datetime", nullable = false)
    private Instant datetime;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransactionType type;

    @Column(precision = 20, scale = 6)
    private BigDecimal amount;

    @Column(precision = 20, scale = 6)
    private BigDecimal fee;

    @Column(precision = 20, scale = 6)
    private BigDecimal tax;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String description;

    // --- Asset relation (present for TRADING, DIVIDEND, STOCKPERK, MIGRATION) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "asset_shares", precision = 20, scale = 10)
    private BigDecimal shares;

    @Column(name = "asset_price", precision = 20, scale = 6)
    private BigDecimal price;

    // --- Counterparty relation (present for CUSTOMER_INBOUND etc.) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_id")
    private Counterparty counterparty;

    @Column(name = "payment_reference", columnDefinition = "TEXT")
    private String paymentReference;

    // --- Merchant name (present for CARD_TRANSACTION*) ---

    @Column(name = "merchant_name")
    private String merchantName;

    // --- FX info (present when original_currency is set) ---

    @Embedded
    private FxInfo fxInfo;

    // --- MCC code (present for CARD_TRANSACTION*) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mcc_code", referencedColumnName = "mcc")
    private MccCode mccCode;

    // --- Account (normalized from source + own_account_iban) ---

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // --- User-defined category (both sources) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_category_id")
    private UserCategory userCategory;

    // --- Sparkasse-specific fields (null for Trade Republic) ---

    @Column(name = "own_account_iban", length = 34)
    private String ownAccountIban;

    @Column(name = "own_account_name")
    private String ownAccountName;

    @Column(name = "sepa_mandate_id")
    private String sepaMandateId;

    @Column(name = "sepa_creditor_id")
    private String sepaCreditorId;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String note;
}
