package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "debit_notes")
@Data
@EqualsAndHashCode(callSuper = true)
public class DebitNote extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String noteNumber;

    @Column(nullable = false)
    private LocalDate noteDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_invoice_id")
    private PurchaseInvoice purchaseInvoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @OneToMany(mappedBy = "debitNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<DebitNoteItem> items = new ArrayList<>();

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebitNoteReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebitNoteStatus status = DebitNoteStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum DebitNoteReason {
        PURCHASE_RETURN, PRICE_CORRECTION, CORRECTION_IN_INVOICE, OTHER
    }

    public enum DebitNoteStatus {
        ACTIVE, CANCELLED
    }
}
