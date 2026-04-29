package com.manuelpuchner.backend.transaction.controller;

import com.manuelpuchner.backend.transaction.dto.CsvImportResult;
import com.manuelpuchner.backend.transaction.dto.TransactionRequest;
import com.manuelpuchner.backend.transaction.dto.TransactionResponse;
import com.manuelpuchner.backend.transaction.dto.TransactionUpdateRequest;
import com.manuelpuchner.backend.transaction.entity.Category;
import com.manuelpuchner.backend.transaction.entity.TransactionType;
import com.manuelpuchner.backend.transaction.service.TransactionService;
import com.manuelpuchner.backend.usercategory.dto.AssignCategoryRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@RequestBody @Valid TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public Page<TransactionResponse> getAll(@PageableDefault(size = 50, sort = {"datetime", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getByTransactionId(@PathVariable UUID transactionId) {
        return service.findByTransactionId(transactionId);
    }

    @GetMapping("/by-category/{category}")
    public Page<TransactionResponse> getByCategory(
            @PathVariable Category category,
            @PageableDefault(size = 50, sort = {"datetime", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByCategory(category, pageable);
    }

    @GetMapping("/by-type/{type}")
    public Page<TransactionResponse> getByType(
            @PathVariable TransactionType type,
            @PageableDefault(size = 50, sort = {"datetime", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByType(type, pageable);
    }

    @GetMapping("/by-mcc/{mcc}")
    public Page<TransactionResponse> getByMcc(
            @PathVariable String mcc,
            @PageableDefault(size = 50, sort = {"datetime", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByMccCode(mcc, pageable);
    }

    @GetMapping("/by-date")
    public Page<TransactionResponse> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50, sort = {"datetime", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByDateRange(from, to, pageable);
    }

    @GetMapping("/by-merchant")
    public Page<TransactionResponse> getByMerchant(
            @RequestParam String q,
            @PageableDefault(size = 50, sort = {"datetime", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.findByMerchant(q, pageable);
    }

    @PostMapping("/import/csv")
    public ResponseEntity<CsvImportResult> importCsv(@RequestParam("file") MultipartFile file)
            throws IOException, com.opencsv.exceptions.CsvException {
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return ResponseEntity.ok(service.importCsv(reader));
        }
    }

    @PostMapping("/import/sparkasse-json")
    public ResponseEntity<CsvImportResult> importSparkasseJson(@RequestParam("file") MultipartFile file)
            throws IOException {
        try (var stream = file.getInputStream()) {
            return ResponseEntity.ok(service.importSparkasseJson(stream));
        }
    }

    @PatchMapping("/{transactionId}/category")
    public TransactionResponse assignCategory(
            @PathVariable UUID transactionId,
            @RequestBody AssignCategoryRequest request) {
        return service.assignUserCategory(transactionId, request.categoryId());
    }

    @PatchMapping("/{transactionId}")
    public TransactionResponse update(
            @PathVariable UUID transactionId,
            @RequestBody TransactionUpdateRequest request) {
        return service.update(transactionId, request);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID transactionId) {
        service.delete(transactionId);
        return ResponseEntity.noContent().build();
    }
}
