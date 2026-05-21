package com.repairshop.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SalesItemRequest {
    private Long productId; // Optional, can be null for custom service
    private String description;
    private String hsn;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal gstPercentage; // Required if productId is null
}
