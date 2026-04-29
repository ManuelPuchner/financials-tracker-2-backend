package com.manuelpuchner.backend.merchantalias.controller;

import com.manuelpuchner.backend.merchantalias.dto.MerchantAliasRequest;
import com.manuelpuchner.backend.merchantalias.dto.MerchantAliasResponse;
import com.manuelpuchner.backend.merchantalias.service.MerchantAliasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant-aliases")
@RequiredArgsConstructor
public class MerchantAliasController {

    private final MerchantAliasService service;

    @GetMapping
    public List<MerchantAliasResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public MerchantAliasResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<MerchantAliasResponse> create(@RequestBody @Valid MerchantAliasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public MerchantAliasResponse update(@PathVariable Long id, @RequestBody @Valid MerchantAliasRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
