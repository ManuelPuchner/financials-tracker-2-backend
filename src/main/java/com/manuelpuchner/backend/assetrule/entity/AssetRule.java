package com.manuelpuchner.backend.assetrule.entity;

import com.manuelpuchner.backend.asset.entity.AssetClass;
import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "asset_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String pattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_field", nullable = false, length = 20)
    private AssetRuleTargetField targetField;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private AssetClass assetClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_category_id", nullable = false)
    private UserCategory userCategory;

    @Column(nullable = false)
    private Integer priority;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
