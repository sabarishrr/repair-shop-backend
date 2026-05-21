package com.repairshop.dto;

import com.repairshop.model.PurchaseInvoice.PurchaseStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseInvoiceRequest {
    private String invoiceNumber;
    private Long supplierId;
    private LocalDate invoiceDate;
    private String notes;
    private PurchaseStatus status;
    private List<PurchaseItemRequest> items;
}
