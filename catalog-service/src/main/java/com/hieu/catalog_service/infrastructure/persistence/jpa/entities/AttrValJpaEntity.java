package com.hieu.catalog_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "attr_vals")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AttrValJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attr_id", nullable = false)
    private AttrJpaEntity attr;

    @Column(nullable = false, length = 100)
    private String val;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
