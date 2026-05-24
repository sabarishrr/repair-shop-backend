package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "credit_notes")
@Data
@EqualsAndHashCode(callSuper = true)
public class CreditNote extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String noteNumber;

    @Column(nullable = false)
    private LocalDate noteDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sales_invoice_id")
    private SalesInvoice salesInvoice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "creditNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<CreditNoteItem> items = new ArrayList<>();

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
    private CreditNoteReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditNoteStatus status = CreditNoteStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum CreditNoteReason {
        SALES_RETURN, POST_SALES_DISCOUNT, CORRECTION_IN_INVOICE, OTHER
    }

    public enum CreditNoteStatus {
        ACTIVE, CANCELLED
    }
}
