package com.manuelpuchner.backend.counterpartymerchant.entity;

import com.manuelpuchner.backend.counterparty.entity.Counterparty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "counterparty_merchant_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterpartyMerchantMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counterparty_id", nullable = false, unique = true)
    private Counterparty counterparty;

    @Column(name = "merchant_name", nullable = false, length = 255)
    private String merchantName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
