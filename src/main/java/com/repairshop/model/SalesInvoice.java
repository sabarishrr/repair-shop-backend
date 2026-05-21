package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales_invoices")
@Data
public class SalesInvoice extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @OneToMany(mappedBy = "salesInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<SalesInvoiceItem> items = new ArrayList<>();

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

    private String deliveryNote;
    private String paymentTerms;
    private String supplierRef;
    private String buyerOrderNo;
    private LocalDate buyerOrderDate;
    private String despatchDocumentNo;
    private LocalDate deliveryNoteDate;
    private String despatchedThrough;
    private String destination;
    private String termsOfDelivery;

    private String salesType; // CASH, CREDIT
    private String paymentMethod; // CASH, CARD, UPI, BANK_TRANSFER
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.PAID;
    
    public enum InvoiceStatus {
        PAID, UNPAID, CANCELLED
    }
}
