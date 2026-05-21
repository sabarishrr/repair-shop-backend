package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
public class InventoryTransaction extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type; // IN or OUT

    @Column(nullable = false)
    private Integer quantity;

    private String referenceType; // e.g., "PURCHASE_INVOICE", "SALES_INVOICE", "MANUAL_ADJUSTMENT"
    private String referenceId; // e.g., "INV-101"

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum TransactionType {
        IN, OUT
    }
}
