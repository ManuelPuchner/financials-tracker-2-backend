package com.manuelpuchner.backend.mcc.controller;

import com.manuelpuchner.backend.mcc.entity.MccCode;
import com.manuelpuchner.backend.mcc.repository.MccCodeRepository;
import com.manuelpuchner.backend.mcc.service.MccCodeService;
import com.manuelpuchner.backend.transaction.dto.MccCodeDto;
import com.manuelpuchner.backend.usercategory.dto.AssignCategoryRequest;
import com.manuelpuchner.backend.usercategory.dto.UserCategoryResponse;
import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mcc")
@RequiredArgsConstructor
public class MccCodeController {

    private final MccCodeRepository mccCodeRepository;
    private final MccCodeService mccCodeService;

    @GetMapping
    @Transactional(readOnly = true)
    public List<MccCodeDto> getAll(@RequestParam(required = false) Boolean mapped) {
        return mccCodeRepository.findAll().stream()
                .filter(m -> mapped == null || (mapped ? m.getUserCategory() != null : m.getUserCategory() == null))
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{mcc}")
    @Transactional(readOnly = true)
    public MccCodeDto getOne(@PathVariable String mcc) {
        return mccCodeRepository.findById(mcc)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("MCC not found: " + mcc));
    }

    @PutMapping("/{mcc}/category")
    public MccCodeDto assignCategory(@PathVariable String mcc, @RequestBody AssignCategoryRequest request) {
        return toDto(mccCodeService.assignCategory(mcc, request.categoryId()));
    }

    private MccCodeDto toDto(MccCode m) {
        UserCategory uc = m.getUserCategory();
        return MccCodeDto.builder()
                .mcc(m.getMcc())
                .description(m.getEditedDescription() != null
                        ? m.getEditedDescription()
                        : m.getCombinedDescription())
                .userCategory(uc != null ? UserCategoryResponse.builder()
                        .id(uc.getId()).name(uc.getName()).color(uc.getColor()).icon(uc.getIcon()).build() : null)
                .build();
    }
}
