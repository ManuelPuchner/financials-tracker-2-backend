package com.manuelpuchner.backend.sparkasserule.controller;

import com.manuelpuchner.backend.sparkasserule.dto.SparkasseRuleRequest;
import com.manuelpuchner.backend.sparkasserule.dto.SparkasseRuleResponse;
import com.manuelpuchner.backend.sparkasserule.service.SparkasseRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sparkasse-rules")
@RequiredArgsConstructor
public class SparkasseRuleController {

    private final SparkasseRuleService service;

    @GetMapping
    public List<SparkasseRuleResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SparkasseRuleResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<SparkasseRuleResponse> create(@RequestBody @Valid SparkasseRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public SparkasseRuleResponse update(@PathVariable Long id, @RequestBody @Valid SparkasseRuleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
