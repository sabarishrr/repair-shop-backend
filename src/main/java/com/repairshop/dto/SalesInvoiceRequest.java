package com.repairshop.dto;

import com.repairshop.model.SalesInvoice.InvoiceStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

@Data
public class SalesInvoiceRequest {
    private String invoiceNumber;
    private Long customerId;
    private Long quotationId; // Optional link
    private LocalDate invoiceDate;
    private String notes;
    private InvoiceStatus status;
    private String salesType;
    private String paymentMethod;
    private BigDecimal receivedAmount;

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

    private List<SalesItemRequest> items;
}
