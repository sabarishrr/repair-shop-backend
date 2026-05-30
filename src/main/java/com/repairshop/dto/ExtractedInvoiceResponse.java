package com.repairshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedInvoiceResponse {
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private Long supplierId;
    private String supplierName;
    private List<ExtractedItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal rate;
    }
}
