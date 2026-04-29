package com.manuelpuchner.backend.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manuelpuchner.backend.transaction.dto.CsvImportResult;
import com.manuelpuchner.backend.transaction.dto.TransactionResponse;
import com.manuelpuchner.backend.transaction.entity.AccountType;
import com.manuelpuchner.backend.transaction.entity.Category;
import com.manuelpuchner.backend.transaction.entity.TransactionType;
import com.manuelpuchner.backend.transaction.service.TransactionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean TransactionService service;

    private final UUID txId = UUID.fromString("ed915aa4-c463-4b3d-bafa-92b54245d5e4");

    private TransactionResponse sampleResponse() {
        return TransactionResponse.builder()
                .id(1L)
                .transactionId(txId)
                .datetime(OffsetDateTime.parse("2024-02-15T13:07:55.080Z"))
                .date(LocalDate.of(2024, 2, 15))
                .accountType(AccountType.DEFAULT)
                .category(Category.TRADING)
                .type(TransactionType.BUY)
                .currency("EUR")
                .build();
    }

    @Test
    void getAll_returnsPage() throws Exception {
        when(service.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value(txId.toString()));
    }

    @Test
    void getByTransactionId_returnsTransaction() throws Exception {
        when(service.findByTransactionId(txId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/transactions/{id}", txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.category").value("TRADING"));
    }

    @Test
    void getByTransactionId_returns404WhenNotFound() throws Exception {
        when(service.findByTransactionId(txId))
                .thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(get("/api/transactions/{id}", txId))
                .andExpect(status().isNotFound());
    }

    @Test
    void importCsv_returnsImportResult() throws Exception {
        CsvImportResult result = CsvImportResult.builder().total(10).imported(8).skipped(2).build();
        when(service.importCsv(any())).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", MediaType.TEXT_PLAIN_VALUE, "csv".getBytes());

        mockMvc.perform(multipart("/api/transactions/import/csv").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.imported").value(8))
                .andExpect(jsonPath("$.skipped").value(2));
    }
}
