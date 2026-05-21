package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_invoices")
@Data
public class PurchaseInvoice extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @OneToMany(mappedBy = "purchaseInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<PurchaseItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalTaxableValue = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalCgst = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalSgst = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalIgst = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    private PurchaseStatus status = PurchaseStatus.RECEIVED;
    
    public enum PurchaseStatus {
        RECEIVED, CANCELLED
    }
}
