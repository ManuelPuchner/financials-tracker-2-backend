package com.manuelpuchner.backend.mcc.service;

import com.manuelpuchner.backend.mcc.entity.MccCode;
import com.manuelpuchner.backend.mcc.repository.MccCodeRepository;
import com.manuelpuchner.backend.recategorize.service.RetroactiveCategorizationService;
import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import com.manuelpuchner.backend.usercategory.repository.UserCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MccCodeService {

    private final MccCodeRepository mccCodeRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final RetroactiveCategorizationService retroactiveService;

    @Transactional
    public MccCode assignCategory(String mcc, Long categoryId) {
        MccCode mccCode = mccCodeRepository.findById(mcc)
                .orElseThrow(() -> new EntityNotFoundException("MCC not found: " + mcc));

        UserCategory category = categoryId != null
                ? userCategoryRepository.findById(categoryId)
                        .orElseThrow(() -> new EntityNotFoundException("Category not found: " + categoryId))
                : null;

        mccCode.setUserCategory(category);
        MccCode saved = mccCodeRepository.save(mccCode);

        if (categoryId != null) {
            retroactiveService.applyMccMapping(mcc, categoryId);
        }

        return saved;
    }
}
