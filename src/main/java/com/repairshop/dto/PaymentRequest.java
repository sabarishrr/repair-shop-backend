package com.repairshop.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRequest {
    private String paymentNumber;
    private LocalDate paymentDate;
    private Long supplierId;
    private Long purchaseInvoiceId;
    private BigDecimal amount;
    private String paymentMethod;
    private String notes;
}
