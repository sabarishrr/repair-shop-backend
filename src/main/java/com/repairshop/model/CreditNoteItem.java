package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "credit_note_items")
@Data
public class CreditNoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_note_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private CreditNote creditNote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product; // Optional, can be null for service/ad-hoc adjustments

    private String description;
    private String hsn;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

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
