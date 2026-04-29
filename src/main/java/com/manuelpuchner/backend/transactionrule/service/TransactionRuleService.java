package com.manuelpuchner.backend.transactionrule.service;

import com.manuelpuchner.backend.recategorize.service.RetroactiveCategorizationService;
import com.manuelpuchner.backend.transactionrule.dto.TransactionRuleRequest;
import com.manuelpuchner.backend.transactionrule.dto.TransactionRuleResponse;
import com.manuelpuchner.backend.transactionrule.entity.TransactionRule;
import com.manuelpuchner.backend.transactionrule.repository.TransactionRuleRepository;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import com.manuelpuchner.backend.usercategory.repository.UserCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Service
public class TransactionRuleService {

    private final TransactionRuleRepository repository;
    private final UserCategoryRepository userCategoryRepository;
    private final RetroactiveCategorizationService retroactiveService;

    private volatile List<CompiledRule> cache = List.of();

    public TransactionRuleService(TransactionRuleRepository repository,
                                  UserCategoryRepository userCategoryRepository,
                                  RetroactiveCategorizationService retroactiveService) {
        this.repository = repository;
        this.userCategoryRepository = userCategoryRepository;
        this.retroactiveService = retroactiveService;
    }

    @Transactional(readOnly = true)
    public List<TransactionRuleResponse> findAll() {
        return repository.findAllByOrderByPriorityAscIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TransactionRuleResponse findById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("TransactionRule not found: " + id));
    }

    @Transactional
    public TransactionRuleResponse create(TransactionRuleRequest request) {
        validatePattern(request.pattern());
        UserCategory category = resolveCategory(request.userCategoryId());
        TransactionRule saved = repository.save(TransactionRule.builder()
                .pattern(request.pattern())
                .targetField(request.targetField())
                .userCategory(category)
                .priority(request.priority() != null ? request.priority() : 100)
                .build());
        invalidateCache();
        retroactiveService.applyTransactionRule(saved);
        return toResponse(saved);
    }

    @Transactional
    public TransactionRuleResponse update(Long id, TransactionRuleRequest request) {
        validatePattern(request.pattern());
        TransactionRule rule = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TransactionRule not found: " + id));
        rule.setPattern(request.pattern());
        rule.setTargetField(request.targetField());
        rule.setUserCategory(resolveCategory(request.userCategoryId()));
        if (request.priority() != null) rule.setPriority(request.priority());
        TransactionRule saved = repository.save(rule);
        invalidateCache();
        retroactiveService.applyTransactionRule(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("TransactionRule not found: " + id);
        }
        repository.deleteById(id);
        invalidateCache();
    }

    public Optional<UserCategory> firstMatch(String partnerName, String counterpartyName, String reference) {
        return getCache().stream()
                .filter(c -> c.matches(partnerName, counterpartyName, reference))
                .map(CompiledRule::category)
                .findFirst();
    }

    private List<CompiledRule> getCache() {
        if (cache.isEmpty()) {
            refreshCache();
        }
        return cache;
    }

    private synchronized void refreshCache() {
        List<CompiledRule> compiled = new ArrayList<>();
        for (TransactionRule rule : repository.findAllByOrderByPriorityAscIdAsc()) {
            try {
                compiled.add(new CompiledRule(Pattern.compile(rule.getPattern()), rule.getTargetField(), rule.getUserCategory()));
            } catch (PatternSyntaxException e) {
                log.warn("Skipping TransactionRule id={} — invalid pattern: {}", rule.getId(), e.getMessage());
            }
        }
        cache = compiled;
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

    private UserCategory resolveCategory(Long id) {
        return userCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("UserCategory not found: " + id));
    }

    private TransactionRuleResponse toResponse(TransactionRule r) {
        UserCategory uc = r.getUserCategory();
        return TransactionRuleResponse.builder()
                .id(r.getId())
                .pattern(r.getPattern())
                .targetField(r.getTargetField())
                .userCategory(UserCategoryResponse.builder()
                        .id(uc.getId())
                        .name(uc.getName())
                        .color(uc.getColor())
                        .icon(uc.getIcon())
                        .build())
                .priority(r.getPriority())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private record CompiledRule(Pattern pattern, com.manuelpuchner.backend.transactionrule.entity.RuleTargetField targetField, UserCategory category) {
        boolean matches(String partnerName, String counterpartyName, String reference) {
            return switch (targetField) {
                case PARTNER_NAME -> partnerName != null && pattern.matcher(partnerName).find();
                case COUNTERPARTY_NAME -> counterpartyName != null && pattern.matcher(counterpartyName).find();
                case REFERENCE -> reference != null && pattern.matcher(reference).find();
                case BOTH -> (partnerName != null && pattern.matcher(partnerName).find())
                        || (counterpartyName != null && pattern.matcher(counterpartyName).find())
                        || (reference != null && pattern.matcher(reference).find());
            };
        }
    }
}
