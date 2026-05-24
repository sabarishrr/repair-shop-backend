package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_details")
@Data
public class CompanyDetails extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName = "TechFix Pro";

    @Column(columnDefinition = "TEXT")
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "state_id")
    private State state;

    private String phone;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    private String gstNumber;

    private String bankName;
    private String accountNumber;
    private String branchIfsCode;
    private String upiId;

    private Long nextInvoiceNo = 1L;
    private Long nextReceiptNo = 1L;
    private Long nextPaymentNo = 1L;
    private Long nextCreditNoteNo = 1L;
    private Long nextDebitNoteNo = 1L;
    
    private String invoicePrefix = "INV-";
    private String receiptPrefix = "REC-";
    private String paymentPrefix = "PAY-";
    private String creditNotePrefix = "CN-";
    private String debitNotePrefix = "DN-";
}
