package com.manuelpuchner.backend.transaction.service;

import com.manuelpuchner.backend.account.entity.Account;
import com.manuelpuchner.backend.account.service.AccountService;
import com.manuelpuchner.backend.asset.entity.Asset;
import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.asset.repository.AssetRepository;
import com.manuelpuchner.backend.counterparty.repository.CounterpartyRepository;
import com.manuelpuchner.backend.mcc.repository.MccCodeRepository;
import com.manuelpuchner.backend.merchantalias.service.MerchantAliasService;
import com.manuelpuchner.backend.sparkasserule.service.SparkasseRuleService;
import com.manuelpuchner.backend.transaction.dto.CsvImportResult;
import com.manuelpuchner.backend.transaction.dto.CsvRow;
import com.manuelpuchner.backend.transaction.dto.TransactionResponse;
import com.manuelpuchner.backend.transaction.entity.*;
import com.manuelpuchner.backend.transaction.repository.TransactionRepository;
import com.manuelpuchner.backend.usercategory.repository.UserCategoryRepository;
import com.opencsv.exceptions.CsvException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AssetRepository assetRepository;
    @Mock CounterpartyRepository counterpartyRepository;
    @Mock MccCodeRepository mccCodeRepository;
    @Mock UserCategoryRepository userCategoryRepository;
    @Mock TransactionCsvParser csvParser;
    @Mock AccountService accountService;
    @Mock MerchantAliasService merchantAliasService;
    @Mock SparkasseRuleService sparkasseRuleService;

    private TransactionService service;
    private final UUID txId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TransactionService(
                transactionRepository, assetRepository, counterpartyRepository,
                mccCodeRepository, userCategoryRepository, new TransactionMapper(),
                csvParser, new com.fasterxml.jackson.databind.ObjectMapper(),
                accountService, merchantAliasService, sparkasseRuleService);
    }

    private Transaction buildTransaction(UUID id) {
        return Transaction.builder()
                .id(1L)
                .transactionId(id)
                .datetime(Instant.now())
                .date(LocalDate.now())
                .accountType(AccountType.DEFAULT)
                .category(Category.TRADING)
                .type(TransactionType.BUY)
                .amount(new BigDecimal("-30.00"))
                .currency("EUR")
                .build();
    }

    @Test
    void findByTransactionId_returnsResponse() {
        when(transactionRepository.findByTransactionId(txId))
                .thenReturn(Optional.of(buildTransaction(txId)));

        TransactionResponse result = service.findByTransactionId(txId);

        assertThat(result.transactionId()).isEqualTo(txId);
        assertThat(result.category()).isEqualTo(Category.TRADING);
    }

    @Test
    void findByTransactionId_throwsWhenNotFound() {
        when(transactionRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByTransactionId(txId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAll_returnsMappedPage() {
        when(transactionRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildTransaction(txId))));

        var page = service.findAll(Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).transactionId()).isEqualTo(txId);
    }

    @Test
    void importCsv_skipsExistingTransactions() throws IOException, CsvException {
        UUID existingId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();

        CsvRow existing = buildCsvRow(existingId, "AAPL");
        CsvRow newRow   = buildCsvRow(newId, "NVDA");

        when(csvParser.parse(any())).thenReturn(List.of(existing, newRow));
        when(transactionRepository.findExistingTransactionIds(any())).thenReturn(Set.of(existingId));
        when(assetRepository.findBySymbolsAsMap(any())).thenReturn(new HashMap<>());
        when(assetRepository.saveAll(any())).thenReturn(List.of());
        when(accountService.resolveOrCreate(any(), any())).thenReturn(Account.builder().id(1L).name("Trade Republic").source(TransactionSource.TRADE_REPUBLIC).build());

        CsvImportResult result = service.importCsv(new StringReader(""));

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    void importCsv_returnsZeroForEmptyFile() throws IOException, CsvException {
        when(csvParser.parse(any())).thenReturn(List.of());

        CsvImportResult result = service.importCsv(new StringReader(""));

        assertThat(result.total()).isEqualTo(0);
        verifyNoInteractions(transactionRepository);
    }

    private CsvRow buildCsvRow(UUID id, String symbol) {
        return new CsvRow(
                Instant.now(), LocalDate.now(), AccountType.DEFAULT,
                Category.TRADING, TransactionType.BUY,
                AssetClass.STOCK, symbol, symbol,
                new BigDecimal("1.0"), new BigDecimal("100.0"),
                new BigDecimal("-100.0"), new BigDecimal("-1.0"), null,
                "EUR", null, null, null, null,
                id, null, null, null, null, null);
    }
}
