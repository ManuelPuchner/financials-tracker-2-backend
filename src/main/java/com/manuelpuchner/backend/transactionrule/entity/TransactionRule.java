package com.manuelpuchner.backend.transactionrule.entity;

import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "sparkasse_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String pattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_field", nullable = false, length = 20)
    private RuleTargetField targetField;

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
