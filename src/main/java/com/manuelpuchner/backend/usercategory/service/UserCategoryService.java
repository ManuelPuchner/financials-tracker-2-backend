package com.manuelpuchner.backend.usercategory.service;

import com.manuelpuchner.backend.usercategory.dto.UserCategoryRequest;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import com.manuelpuchner.backend.usercategory.repository.UserCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCategoryService {

    private final UserCategoryRepository repository;

    @Transactional
    public UserCategoryResponse create(UserCategoryRequest req) {
        if (repository.existsByNameIgnoreCase(req.name())) {
            throw new IllegalArgumentException("Category already exists: " + req.name());
        }
        UserCategory saved = repository.save(UserCategory.builder()
                .name(req.name())
                .color(req.color())
                .icon(req.icon())
                .build());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserCategoryResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserCategoryResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public UserCategoryResponse update(Long id, UserCategoryRequest req) {
        UserCategory category = getOrThrow(id);
        if (!category.getName().equalsIgnoreCase(req.name())
                && repository.existsByNameIgnoreCase(req.name())) {
            throw new IllegalArgumentException("Category already exists: " + req.name());
        }
        category.setName(req.name());
        category.setColor(req.color());
        category.setIcon(req.icon());
        return toResponse(repository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Category not found: " + id);
        }
        repository.deleteById(id);
    }

    public UserCategory getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
    }

    public UserCategoryResponse toResponse(UserCategory c) {
        return UserCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .color(c.getColor())
                .icon(c.getIcon())
                .build();
    }
}
