package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "sales_invoice_items")
@Data
public class SalesInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private SalesInvoice salesInvoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product; // Optional, can be null for ad-hoc service charges

    private String description; // Fallback if no product, or custom description
    private String hsn;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal gstPercentage = BigDecimal.ZERO;

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
