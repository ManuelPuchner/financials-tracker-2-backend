package com.manuelpuchner.backend.transaction.service;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.transaction.dto.CsvRow;
import com.manuelpuchner.backend.transaction.entity.Category;
import com.manuelpuchner.backend.transaction.entity.TransactionType;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionCsvParserTest {

    private final TransactionCsvParser parser = new TransactionCsvParser();

    private static final String HEADER = "\"datetime\",\"date\",\"account_type\",\"category\",\"type\",\"asset_class\",\"name\",\"symbol\",\"shares\",\"price\",\"amount\",\"fee\",\"tax\",\"currency\",\"original_amount\",\"original_currency\",\"fx_rate\",\"description\",\"transaction_id\",\"counterparty_name\",\"counterparty_iban\",\"payment_reference\",\"mcc_code\"";

    @Test
    void parsesTradeRow() throws IOException, CsvException {
        String csv = HEADER + "\n" +
                "\"2024-02-15T13:07:55.080Z\",\"2024-02-15\",\"DEFAULT\",\"TRADING\",\"BUY\",\"FUND\",\"S&P 500 EUR (Acc)\",\"LU1681048804\",\"0.3309210000\",\"90.656000\",\"-30.00\",\"-1.00\",\"\",\"EUR\",\"\",\"\",\"\",\"\",\"ed915aa4-c463-4b3d-bafa-92b54245d5e4\",\"\",\"\",\"\",\"\"";

        List<CsvRow> result = parser.parse(new StringReader(csv));

        assertThat(result).hasSize(1);
        CsvRow row = result.get(0);
        assertThat(row.category()).isEqualTo(Category.TRADING);
        assertThat(row.type()).isEqualTo(TransactionType.BUY);
        assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("-30.00"));
        assertThat(row.fee()).isEqualByComparingTo(new BigDecimal("-1.00"));
        assertThat(row.assetClass()).isEqualTo(AssetClass.FUND);
        assertThat(row.assetSymbol()).isEqualTo("LU1681048804");
        assertThat(row.assetName()).isEqualTo("S&P 500 EUR (Acc)");
        assertThat(row.merchantName()).isNull();
        assertThat(row.fxOriginalCurrency()).isNull();
        assertThat(row.counterpartyIban()).isNull();
        assertThat(row.mccCode()).isNull();
    }

    @Test
    void parsesCashRowWithCounterparty() throws IOException, CsvException {
        String csv = HEADER + "\n" +
                "\"2024-02-15T12:27:32.803282Z\",\"2024-02-15\",\"DEFAULT\",\"CASH\",\"CUSTOMER_INBOUND\",\"\",\"Manuel Puchner\",\"\",\"\",\"\",\"50.000000\",\"\",\"\",\"EUR\",\"\",\"\",\"\",\"No SEPA description provided\",\"44af0eed-65e3-4134-9d18-821bc32dd221\",\"Manuel Puchner\",\"AT832032032202757965\",\"\",\"\"";

        List<CsvRow> result = parser.parse(new StringReader(csv));

        assertThat(result).hasSize(1);
        CsvRow row = result.get(0);
        assertThat(row.counterpartyIban()).isEqualTo("AT832032032202757965");
        assertThat(row.counterpartyName()).isEqualTo("Manuel Puchner");
        assertThat(row.assetClass()).isNull();
        assertThat(row.assetSymbol()).isNull();
        assertThat(row.merchantName()).isNull();
    }

    @Test
    void parsesRowWithFxInfo() throws IOException, CsvException {
        String csv = HEADER + "\n" +
                "\"2024-05-16T03:46:52.176604Z\",\"2024-05-16\",\"DEFAULT\",\"CASH\",\"DIVIDEND\",\"STOCK\",\"Apple\",\"US0378331005\",\"0.0583000000\",\"\",\"0.010000\",\"\",\"\",\"EUR\",\"0.01\",\"USD\",\"1.080200\",\"\",\"d69817cb-e83d-464c-92b3-427c12a1a00a\",\"\",\"\",\"\",\"\"";

        List<CsvRow> result = parser.parse(new StringReader(csv));

        assertThat(result).hasSize(1);
        CsvRow row = result.get(0);
        assertThat(row.fxOriginalCurrency()).isEqualTo("USD");
        assertThat(row.fxRate()).isEqualByComparingTo(new BigDecimal("1.080200"));
    }

    @Test
    void cardTransactionStoresMerchantNameNotAssetName() throws IOException, CsvException {
        String csv = HEADER + "\n" +
                "\"2024-05-18T09:31:19.028412Z\",\"2024-05-18\",\"DEFAULT\",\"CASH\",\"CARD_TRANSACTION\",\"\",\"McDonalds 126\",\"\",\"\",\"\",\"-3.300000\",\"\",\"\",\"EUR\",\"\",\"\",\"\",\"TR Card Transaction\",\"7aa313be-ba36-4f04-9bb4-6bdee6013541\",\"\",\"\",\"\",\"5812\"";

        List<CsvRow> result = parser.parse(new StringReader(csv));

        assertThat(result).hasSize(1);
        CsvRow row = result.get(0);
        assertThat(row.merchantName()).isEqualTo("McDonalds 126");
        assertThat(row.assetName()).isNull();
        assertThat(row.assetClass()).isNull();
        assertThat(row.mccCode()).isEqualTo("5812");
    }

    @Test
    void returnsEmptyListForHeaderOnly() throws IOException, CsvException {
        assertThat(parser.parse(new StringReader(HEADER))).isEmpty();
    }
}
