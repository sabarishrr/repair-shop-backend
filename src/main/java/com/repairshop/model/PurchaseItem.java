package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "purchase_items")
@Data
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_invoice_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private PurchaseInvoice purchaseInvoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal rate;

    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal taxableValue = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;
}
