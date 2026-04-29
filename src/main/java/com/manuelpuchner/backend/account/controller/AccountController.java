package com.manuelpuchner.backend.account.controller;

import com.manuelpuchner.backend.account.dto.AccountResponse;
import com.manuelpuchner.backend.account.dto.AccountUpdateRequest;
import com.manuelpuchner.backend.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> getAll() {
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable Long id) {
        return accountService.findById(id);
    }

    @PatchMapping("/{id}")
    public AccountResponse update(@PathVariable Long id, @RequestBody AccountUpdateRequest request) {
        return accountService.update(id, request);
    }
}
