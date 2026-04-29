package com.manuelpuchner.backend.usercategory.controller;

import com.manuelpuchner.backend.usercategory.dto.UserCategoryRequest;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import com.manuelpuchner.backend.usercategory.service.UserCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class UserCategoryController {

    private final UserCategoryService service;

    @PostMapping
    public ResponseEntity<UserCategoryResponse> create(@RequestBody @Valid UserCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<UserCategoryResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserCategoryResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public UserCategoryResponse update(@PathVariable Long id, @RequestBody @Valid UserCategoryRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
