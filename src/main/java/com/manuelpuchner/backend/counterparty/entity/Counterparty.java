package com.manuelpuchner.backend.counterparty.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "counterparties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Counterparty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 34)
    private String iban;

    @Column
    private String name;
}
