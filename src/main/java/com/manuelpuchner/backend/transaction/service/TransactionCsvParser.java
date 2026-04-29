package com.manuelpuchner.backend.transaction.service;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.transaction.dto.CsvRow;
import com.manuelpuchner.backend.transaction.entity.AccountType;
import com.manuelpuchner.backend.transaction.entity.Category;
import com.manuelpuchner.backend.transaction.entity.TransactionType;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionCsvParser {

    // Column indices matching Trade Republic CSV export
    private static final int COL_DATETIME          = 0;
    private static final int COL_DATE              = 1;
    private static final int COL_ACCOUNT_TYPE      = 2;
    private static final int COL_CATEGORY          = 3;
    private static final int COL_TYPE              = 4;
    private static final int COL_ASSET_CLASS       = 5;
    private static final int COL_NAME              = 6;
    private static final int COL_SYMBOL            = 7;
    private static final int COL_SHARES            = 8;
    private static final int COL_PRICE             = 9;
    private static final int COL_AMOUNT            = 10;
    private static final int COL_FEE               = 11;
    private static final int COL_TAX               = 12;
    private static final int COL_CURRENCY          = 13;
    private static final int COL_FX_AMOUNT         = 14;
    private static final int COL_FX_CURRENCY       = 15;
    private static final int COL_FX_RATE           = 16;
    private static final int COL_DESCRIPTION       = 17;
    private static final int COL_TRANSACTION_ID    = 18;
    private static final int COL_COUNTERPARTY_NAME = 19;
    private static final int COL_COUNTERPARTY_IBAN = 20;
    private static final int COL_PAYMENT_REF       = 21;
    private static final int COL_MCC               = 22;

    public List<CsvRow> parse(Reader reader) throws IOException, CsvException {
        try (CSVReader csv = new CSVReader(reader)) {
            List<String[]> rows = csv.readAll();
            if (rows.size() < 2) return List.of();

            List<CsvRow> result = new ArrayList<>(rows.size() - 1);
            for (int i = 1; i < rows.size(); i++) {
                result.add(mapRow(rows.get(i)));
            }
            return result;
        }
    }

    private CsvRow mapRow(String[] r) {
        AssetClass assetClass = parseEnum(AssetClass.class, r[COL_ASSET_CLASS]);
        String name = blankToNull(r[COL_NAME]);

        // For card transactions the name column holds the merchant, not an asset name
        boolean isCardTransaction = isCardTransaction(r[COL_TYPE]);
        String assetName    = (!isCardTransaction && assetClass != null) ? name : null;
        String merchantName = isCardTransaction ? name : null;

        return new CsvRow(
                Instant.parse(r[COL_DATETIME]),
                LocalDate.parse(r[COL_DATE]),
                AccountType.valueOf(r[COL_ACCOUNT_TYPE]),
                Category.valueOf(r[COL_CATEGORY]),
                TransactionType.valueOf(r[COL_TYPE]),
                assetClass,
                assetName,
                blankToNull(r[COL_SYMBOL]),
                parseBigDecimal(r[COL_SHARES]),
                parseBigDecimal(r[COL_PRICE]),
                parseBigDecimal(r[COL_AMOUNT]),
                parseBigDecimal(r[COL_FEE]),
                parseBigDecimal(r[COL_TAX]),
                r[COL_CURRENCY],
                parseBigDecimal(r[COL_FX_AMOUNT]),
                blankToNull(r[COL_FX_CURRENCY]),
                parseBigDecimal(r[COL_FX_RATE]),
                blankToNull(r[COL_DESCRIPTION]),
                UUID.fromString(r[COL_TRANSACTION_ID]),
                blankToNull(r[COL_COUNTERPARTY_NAME]),
                blankToNull(r[COL_COUNTERPARTY_IBAN]),
                blankToNull(r[COL_PAYMENT_REF]),
                blankToNull(r[COL_MCC]),
                merchantName
        );
    }

    private boolean isCardTransaction(String type) {
        return "CARD_TRANSACTION".equals(type) || "CARD_TRANSACTION_INTERNATIONAL".equals(type);
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        return new BigDecimal(s);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> cls, String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Enum.valueOf(cls, s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
