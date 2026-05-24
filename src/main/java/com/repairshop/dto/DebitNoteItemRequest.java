package com.repairshop.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DebitNoteItemRequest {
    private Long productId; // Optional, null for ad-hoc supplier return or vendor adjustment
    private String description;
    private String hsn;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal gstPercentage;
}
