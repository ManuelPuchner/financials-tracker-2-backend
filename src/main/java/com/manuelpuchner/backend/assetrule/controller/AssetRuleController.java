package com.manuelpuchner.backend.assetrule.controller;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.assetrule.dto.AssetRuleRequest;
import com.manuelpuchner.backend.assetrule.dto.AssetRuleResponse;
import com.manuelpuchner.backend.assetrule.service.AssetRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asset-rules")
@RequiredArgsConstructor
public class AssetRuleController {

    private final AssetRuleService service;

    @GetMapping
    public List<AssetRuleResponse> getAll(@RequestParam(required = false) AssetClass assetClass) {
        return service.findAll(assetClass);
    }

    @GetMapping("/{id}")
    public AssetRuleResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<AssetRuleResponse> create(@RequestBody @Valid AssetRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public AssetRuleResponse update(@PathVariable Long id, @RequestBody @Valid AssetRuleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
