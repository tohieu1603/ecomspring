package com.hieu.catalog_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variant_attrs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VariantAttrJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private VariantJpaEntity variant;

    @Column(name = "attr_id", nullable = false)
    private Long attrId;

    @Column(name = "attr_code", nullable = false, length = 64)
    private String attrCode;

    @Column(name = "attr_name", nullable = false, length = 100)
    private String attrName;

    @Column(name = "val_id")
    private Long valId;

    @Column(name = "val_text", length = 255)
    private String valText;
}
