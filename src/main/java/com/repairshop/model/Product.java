package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal rate;

    private String hsn;

    @Column(nullable = false)
    private BigDecimal gstPercentage;

    @Column(nullable = false)
    private Integer stockQuantity = 0;

    private String uom = "NOS";

    private BigDecimal purchasePrice;

    private BigDecimal mrp;

    private BigDecimal wholesalePrice;

    private Integer reorderLevel = 0;
}
