package com.manuelpuchner.backend.account.service;

import com.manuelpuchner.backend.account.dto.AccountResponse;
import com.manuelpuchner.backend.account.dto.AccountUpdateRequest;
import com.manuelpuchner.backend.account.entity.Account;
import com.manuelpuchner.backend.account.repository.AccountRepository;
import com.manuelpuchner.backend.transaction.entity.TransactionSource;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(Long id) {
        return accountRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
    }

    @Transactional
    public AccountResponse update(Long id, AccountUpdateRequest request) {
        if (request.name() == null && request.color() == null && request.icon() == null) {
            throw new IllegalArgumentException("At least one field must be provided");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
        if (request.name() != null) account.setName(request.name());
        if (request.color() != null) account.setColor(request.color());
        if (request.icon() != null) account.setIcon(request.icon());
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public Account resolveOrCreate(TransactionSource source, String ownAccountIban) {
        try {
            return accountRepository.findBySourceAndOwnAccountIban(source, ownAccountIban)
                    .orElseGet(() -> accountRepository.save(Account.builder()
                            .source(source)
                            .ownAccountIban(ownAccountIban)
                            .name(defaultName(source, ownAccountIban))
                            .build()));
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread inserted first
            return accountRepository.findBySourceAndOwnAccountIban(source, ownAccountIban)
                    .orElseThrow(() -> new IllegalStateException("Account race condition unresolvable", e));
        }
    }

    private String defaultName(TransactionSource source, String iban) {
        if (source == TransactionSource.TRADE_REPUBLIC) return "Trade Republic";
        if (source == TransactionSource.SPARKASSE && iban != null && iban.length() >= 4) {
            return "Sparkasse " + iban.substring(iban.length() - 4);
        }
        return source.name();
    }

    private AccountResponse toResponse(Account a) {
        return AccountResponse.builder()
                .id(a.getId())
                .name(a.getName())
                .color(a.getColor())
                .icon(a.getIcon())
                .source(a.getSource())
                .ownAccountIban(a.getOwnAccountIban())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
