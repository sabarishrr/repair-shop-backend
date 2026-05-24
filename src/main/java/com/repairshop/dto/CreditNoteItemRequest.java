package com.repairshop.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreditNoteItemRequest {
    private Long productId; // Optional, null for ad-hoc service adjustment
    private String description;
    private String hsn;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal gstPercentage;
}
