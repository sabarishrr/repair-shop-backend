package com.repairshop.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReceiptRequest {
    private String receiptNumber;
    private LocalDate receiptDate;
    private Long customerId;
    private Long salesInvoiceId;
    private BigDecimal amount;
    private String paymentMethod;
    private String notes;
}
