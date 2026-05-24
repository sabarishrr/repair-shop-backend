package com.repairshop.dto;

import com.repairshop.model.Product;
import com.repairshop.model.SalesInvoice;
import com.repairshop.model.PurchaseInvoice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private SalesReportDTO salesReport;
    private PurchaseReportDTO purchaseReport;
    private JobSheetReportDTO jobSheetReport;
    private InventoryReportDTO inventoryReport;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesReportDTO {
        private long totalSalesCount;
        private BigDecimal totalSalesAmount;
        private long totalRefundsCount;
        private BigDecimal totalRefundsAmount;
        private BigDecimal netSalesAmount;
        private BigDecimal totalReceiptsAmount;
        private BigDecimal cashSalesAmount;
        private BigDecimal creditSalesAmount;
        private Map<String, BigDecimal> paymentMethodBreakdown;
        private TaxSplitDTO taxSplit;
        private List<SalesInvoice> invoices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseReportDTO {
        private long totalPurchasesCount;
        private BigDecimal totalPurchasesAmount;
        private long totalDebitNotesCount;
        private BigDecimal totalDebitNotesAmount;
        private BigDecimal netPurchasesAmount;
        private BigDecimal totalPaymentsAmount;
        private TaxSplitDTO taxSplit;
        private List<PurchaseInvoice> invoices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobSheetReportDTO {
        private long totalJobsCreated;
        private Map<String, Long> statusCounts;
        private Map<String, Long> brandCounts;
        private List<TechnicianPerformanceDTO> technicianPerformance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryReportDTO {
        private BigDecimal totalStockValuationPurchase;
        private BigDecimal totalStockValuationSale;
        private long lowStockCount;
        private List<Product> lowStockItems;
        private List<TopProductDTO> topSellingSpares;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxSplitDTO {
        private BigDecimal taxableValue = BigDecimal.ZERO;
        private BigDecimal cgst = BigDecimal.ZERO;
        private BigDecimal sgst = BigDecimal.ZERO;
        private BigDecimal igst = BigDecimal.ZERO;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicianPerformanceDTO {
        private String technician;
        private long jobCount;
        private BigDecimal totalEstimatedCost;
        private BigDecimal totalFinalCost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductDTO {
        private String productName;
        private long quantitySold;
        private BigDecimal totalRevenue;
    }
}
