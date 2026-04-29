package com.manuelpuchner.backend.mcc.entity;

import com.manuelpuchner.backend.usercategory.entity.UserCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mcc_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MccCode {

    @Id
    @Column(length = 4)
    private String mcc;

    @Column(name = "edited_description")
    private String editedDescription;

    @Column(name = "combined_description")
    private String combinedDescription;

    @Column(name = "usda_description")
    private String usdaDescription;

    @Column(name = "irs_description")
    private String irsDescription;

    @Column(name = "irs_reportable")
    private Boolean irsReportable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_category_id")
    private UserCategory userCategory;
}
