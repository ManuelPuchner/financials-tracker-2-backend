package com.manuelpuchner.backend.assetrule.service;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.assetrule.dto.AssetRuleRequest;
import com.manuelpuchner.backend.assetrule.dto.AssetRuleResponse;
import com.manuelpuchner.backend.assetrule.entity.AssetRule;
import com.manuelpuchner.backend.assetrule.entity.AssetRuleTargetField;
import com.manuelpuchner.backend.assetrule.repository.AssetRuleRepository;
import com.manuelpuchner.backend.recategorize.service.RetroactiveCategorizationService;
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
public class AssetRuleService {

    private final AssetRuleRepository repository;
    private final UserCategoryRepository userCategoryRepository;
    private final RetroactiveCategorizationService retroactiveService;

    private volatile List<CompiledRule> cache = List.of();

    public AssetRuleService(AssetRuleRepository repository,
                            UserCategoryRepository userCategoryRepository,
                            RetroactiveCategorizationService retroactiveService) {
        this.repository = repository;
        this.userCategoryRepository = userCategoryRepository;
        this.retroactiveService = retroactiveService;
    }

    @Transactional(readOnly = true)
    public List<AssetRuleResponse> findAll(AssetClass assetClass) {
        List<AssetRule> rules = assetClass != null
                ? repository.findAllByAssetClassOrderByPriorityAscIdAsc(assetClass)
                : repository.findAllByOrderByPriorityAscIdAsc();
        return rules.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AssetRuleResponse findById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("AssetRule not found: " + id));
    }

    @Transactional
    public AssetRuleResponse create(AssetRuleRequest request) {
        validatePattern(request.pattern());
        AssetRule saved = repository.save(AssetRule.builder()
                .pattern(request.pattern())
                .targetField(request.targetField())
                .assetClass(request.assetClass())
                .userCategory(resolveCategory(request.userCategoryId()))
                .priority(request.priority() != null ? request.priority() : 100)
                .build());
        invalidateCache();
        retroactiveService.applyAssetRule(saved);
        return toResponse(saved);
    }

    @Transactional
    public AssetRuleResponse update(Long id, AssetRuleRequest request) {
        validatePattern(request.pattern());
        AssetRule rule = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AssetRule not found: " + id));
        rule.setPattern(request.pattern());
        rule.setTargetField(request.targetField());
        rule.setAssetClass(request.assetClass());
        rule.setUserCategory(resolveCategory(request.userCategoryId()));
        if (request.priority() != null) rule.setPriority(request.priority());
        AssetRule saved = repository.save(rule);
        invalidateCache();
        retroactiveService.applyAssetRule(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("AssetRule not found: " + id);
        }
        repository.deleteById(id);
        invalidateCache();
    }

    public Optional<UserCategory> firstMatch(AssetClass assetClass, String symbol, String name) {
        return getCache().stream()
                .filter(c -> c.assetClass() == assetClass && c.matches(symbol, name))
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
        for (AssetRule rule : repository.findAllByOrderByPriorityAscIdAsc()) {
            try {
                compiled.add(new CompiledRule(
                        Pattern.compile(rule.getPattern()),
                        rule.getTargetField(),
                        rule.getAssetClass(),
                        rule.getUserCategory()));
            } catch (PatternSyntaxException e) {
                log.warn("Skipping AssetRule id={} — invalid pattern: {}", rule.getId(), e.getMessage());
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

    private AssetRuleResponse toResponse(AssetRule r) {
        UserCategory uc = r.getUserCategory();
        return AssetRuleResponse.builder()
                .id(r.getId())
                .pattern(r.getPattern())
                .targetField(r.getTargetField())
                .assetClass(r.getAssetClass())
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

    private record CompiledRule(Pattern pattern, AssetRuleTargetField targetField, AssetClass assetClass, UserCategory category) {
        boolean matches(String symbol, String name) {
            return switch (targetField) {
                case SYMBOL -> symbol != null && pattern.matcher(symbol).find();
                case NAME -> name != null && pattern.matcher(name).find();
                case BOTH -> (symbol != null && pattern.matcher(symbol).find())
                        || (name != null && pattern.matcher(name).find());
            };
        }
    }
}
