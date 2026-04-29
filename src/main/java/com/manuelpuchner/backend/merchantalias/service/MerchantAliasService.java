package com.manuelpuchner.backend.merchantalias.service;

import com.manuelpuchner.backend.merchantalias.dto.MerchantAliasRequest;
import com.manuelpuchner.backend.merchantalias.dto.MerchantAliasResponse;
import com.manuelpuchner.backend.merchantalias.entity.MerchantAlias;
import com.manuelpuchner.backend.merchantalias.repository.MerchantAliasRepository;
import com.manuelpuchner.backend.recategorize.service.RetroactiveCategorizationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Service
public class MerchantAliasService {

    private final MerchantAliasRepository repository;
    private final RetroactiveCategorizationService retroactiveService;

    private volatile List<CompiledAlias> cache = List.of();
    private final AtomicLong cacheVersion = new AtomicLong(0);

    public MerchantAliasService(MerchantAliasRepository repository,
                                RetroactiveCategorizationService retroactiveService) {
        this.repository = repository;
        this.retroactiveService = retroactiveService;
    }

    @Transactional(readOnly = true)
    public List<MerchantAliasResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MerchantAliasResponse findById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("MerchantAlias not found: " + id));
    }

    @Transactional
    public MerchantAliasResponse create(MerchantAliasRequest request) {
        validatePattern(request.pattern());
        MerchantAlias saved = repository.save(MerchantAlias.builder()
                .pattern(request.pattern())
                .canonicalName(request.canonicalName())
                .build());
        invalidateCache();
        retroactiveService.applyMerchantAlias(saved);
        return toResponse(saved);
    }

    @Transactional
    public MerchantAliasResponse update(Long id, MerchantAliasRequest request) {
        validatePattern(request.pattern());
        MerchantAlias alias = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MerchantAlias not found: " + id));
        alias.setPattern(request.pattern());
        alias.setCanonicalName(request.canonicalName());
        MerchantAlias saved = repository.save(alias);
        invalidateCache();
        retroactiveService.applyMerchantAlias(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("MerchantAlias not found: " + id);
        }
        repository.deleteById(id);
        invalidateCache();
    }

    public Optional<String> resolve(String rawMerchantName) {
        if (rawMerchantName == null) return Optional.empty();
        return getCache().stream()
                .filter(c -> c.pattern().matcher(rawMerchantName).find())
                .map(CompiledAlias::canonicalName)
                .findFirst();
    }

    private List<CompiledAlias> getCache() {
        if (cache.isEmpty()) {
            refreshCache();
        }
        return cache;
    }

    private synchronized void refreshCache() {
        cache = repository.findAll().stream()
                .map(a -> new CompiledAlias(Pattern.compile(a.getPattern()), a.getCanonicalName()))
                .toList();
    }

    private void invalidateCache() {
        cache = List.of();
    }

    private void validatePattern(String pattern) {
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid regex pattern: " + e.getMessage());
        }
    }

    private MerchantAliasResponse toResponse(MerchantAlias a) {
        return MerchantAliasResponse.builder()
                .id(a.getId())
                .pattern(a.getPattern())
                .canonicalName(a.getCanonicalName())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private record CompiledAlias(Pattern pattern, String canonicalName) {}
}
