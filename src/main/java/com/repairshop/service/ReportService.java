package com.repairshop.service;

import com.repairshop.dto.ReportSummaryResponse;
import com.repairshop.dto.ReportSummaryResponse.*;
import com.repairshop.model.*;
import com.repairshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final DebitNoteRepository debitNoteRepository;
    private final JobSheetRepository jobSheetRepository;
    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    public ReportSummaryResponse getReportSummary(LocalDate startDate, LocalDate endDate) {
        SalesReportDTO salesReport = computeSalesReport(startDate, endDate);
        PurchaseReportDTO purchaseReport = computePurchaseReport(startDate, endDate);
        JobSheetReportDTO jobSheetReport = computeJobSheetReport(startDate, endDate);
        InventoryReportDTO inventoryReport = computeInventoryReport(startDate, endDate);

        return new ReportSummaryResponse(startDate, endDate, salesReport, purchaseReport, jobSheetReport, inventoryReport);
    }

    private SalesReportDTO computeSalesReport(LocalDate startDate, LocalDate endDate) {
        List<SalesInvoice> invoices = salesInvoiceRepository.findByInvoiceDateBetweenOrderByInvoiceDateDesc(startDate, endDate);
        List<CreditNote> creditNotes = creditNoteRepository.findByNoteDateBetweenOrderByNoteDateDesc(startDate, endDate);
        List<Receipt> receipts = receiptRepository.findByReceiptDateBetweenOrderByReceiptDateDesc(startDate, endDate);

        List<SalesInvoice> activeInvoices = invoices.stream()
                .filter(i -> i.getStatus() != SalesInvoice.InvoiceStatus.CANCELLED)
                .collect(Collectors.toList());

        List<CreditNote> activeCreditNotes = creditNotes.stream()
                .filter(c -> c.getStatus() != CreditNote.CreditNoteStatus.CANCELLED)
                .collect(Collectors.toList());

        long totalSalesCount = activeInvoices.size();
        BigDecimal totalSalesAmount = activeInvoices.stream()
                .map(SalesInvoice::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalRefundsCount = activeCreditNotes.size();
        BigDecimal totalRefundsAmount = activeCreditNotes.stream()
                .map(CreditNote::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSalesAmount = totalSalesAmount.subtract(totalRefundsAmount);

        BigDecimal totalReceiptsAmount = receipts.stream()
                .map(Receipt::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashSalesAmount = activeInvoices.stream()
                .filter(i -> "CASH".equalsIgnoreCase(i.getSalesType()))
                .map(SalesInvoice::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal creditSalesAmount = activeInvoices.stream()
                .filter(i -> "CREDIT".equalsIgnoreCase(i.getSalesType()))
                .map(SalesInvoice::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Grouping payment methods
        Map<String, BigDecimal> paymentMethodBreakdown = new HashMap<>();
        // Seed default keys to avoid UI issues
        paymentMethodBreakdown.put("CASH", BigDecimal.ZERO);
        paymentMethodBreakdown.put("CARD", BigDecimal.ZERO);
        paymentMethodBreakdown.put("UPI", BigDecimal.ZERO);
        paymentMethodBreakdown.put("BANK_TRANSFER", BigDecimal.ZERO);

        for (SalesInvoice si : activeInvoices) {
            String method = si.getPaymentMethod();
            if (method != null && !method.trim().isEmpty()) {
                String key = method.toUpperCase().trim();
                paymentMethodBreakdown.put(key, paymentMethodBreakdown.getOrDefault(key, BigDecimal.ZERO).add(si.getGrandTotal()));
            }
        }

        // Tax split
        BigDecimal taxable = activeInvoices.stream().map(SalesInvoice::getTotalTaxableValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cgst = activeInvoices.stream().map(SalesInvoice::getTotalCgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sgst = activeInvoices.stream().map(SalesInvoice::getTotalSgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igst = activeInvoices.stream().map(SalesInvoice::getTotalIgst).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Deduct credit notes tax splits from sales tax splits
        BigDecimal cnTaxable = activeCreditNotes.stream().map(CreditNote::getTotalTaxableValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cnCgst = activeCreditNotes.stream().map(CreditNote::getTotalCgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cnSgst = activeCreditNotes.stream().map(CreditNote::getTotalSgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cnIgst = activeCreditNotes.stream().map(CreditNote::getTotalIgst).reduce(BigDecimal.ZERO, BigDecimal::add);

        TaxSplitDTO taxSplit = new TaxSplitDTO(
                taxable.subtract(cnTaxable),
                cgst.subtract(cnCgst),
                sgst.subtract(cnSgst),
                igst.subtract(cnIgst)
        );

        return new SalesReportDTO(
                totalSalesCount,
                totalSalesAmount,
                totalRefundsCount,
                totalRefundsAmount,
                netSalesAmount,
                totalReceiptsAmount,
                cashSalesAmount,
                creditSalesAmount,
                paymentMethodBreakdown,
                taxSplit,
                invoices
        );
    }

    private PurchaseReportDTO computePurchaseReport(LocalDate startDate, LocalDate endDate) {
        List<PurchaseInvoice> purchases = purchaseInvoiceRepository.findByInvoiceDateBetweenOrderByInvoiceDateDesc(startDate, endDate);
        List<DebitNote> debitNotes = debitNoteRepository.findByNoteDateBetweenOrderByNoteDateDesc(startDate, endDate);
        List<Payment> payments = paymentRepository.findByPaymentDateBetweenOrderByPaymentDateDesc(startDate, endDate);

        List<PurchaseInvoice> activePurchases = purchases.stream()
                .filter(p -> p.getStatus() != PurchaseInvoice.PurchaseStatus.CANCELLED)
                .collect(Collectors.toList());

        List<DebitNote> activeDebitNotes = debitNotes.stream()
                .filter(d -> d.getStatus() != DebitNote.DebitNoteStatus.CANCELLED)
                .collect(Collectors.toList());

        long totalPurchasesCount = activePurchases.size();
        BigDecimal totalPurchasesAmount = activePurchases.stream()
                .map(PurchaseInvoice::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalDebitNotesCount = activeDebitNotes.size();
        BigDecimal totalDebitNotesAmount = activeDebitNotes.stream()
                .map(DebitNote::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netPurchasesAmount = totalPurchasesAmount.subtract(totalDebitNotesAmount);

        BigDecimal totalPaymentsAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tax split
        BigDecimal taxable = activePurchases.stream().map(PurchaseInvoice::getTotalTaxableValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cgst = activePurchases.stream().map(PurchaseInvoice::getTotalCgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sgst = activePurchases.stream().map(PurchaseInvoice::getTotalSgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igst = activePurchases.stream().map(PurchaseInvoice::getTotalIgst).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Deduct debit notes tax splits from purchase tax splits
        BigDecimal dnTaxable = activeDebitNotes.stream().map(DebitNote::getTotalTaxableValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dnCgst = activeDebitNotes.stream().map(DebitNote::getTotalCgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dnSgst = activeDebitNotes.stream().map(DebitNote::getTotalSgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dnIgst = activeDebitNotes.stream().map(DebitNote::getTotalIgst).reduce(BigDecimal.ZERO, BigDecimal::add);

        TaxSplitDTO taxSplit = new TaxSplitDTO(
                taxable.subtract(dnTaxable),
                cgst.subtract(dnCgst),
                sgst.subtract(dnSgst),
                igst.subtract(dnIgst)
        );

        return new PurchaseReportDTO(
                totalPurchasesCount,
                totalPurchasesAmount,
                totalDebitNotesCount,
                totalDebitNotesAmount,
                netPurchasesAmount,
                totalPaymentsAmount,
                taxSplit,
                purchases
        );
    }

    private JobSheetReportDTO computeJobSheetReport(LocalDate startDate, LocalDate endDate) {
        List<JobSheet> jobs = jobSheetRepository.findByReceivedDateBetweenOrderByReceivedDateDesc(startDate, endDate);

        long totalJobsCreated = jobs.size();

        // Status Counts
        Map<String, Long> statusCounts = new HashMap<>();
        for (JobStatus js : JobStatus.values()) {
            statusCounts.put(js.name(), 0L);
        }
        for (JobSheet job : jobs) {
            if (job.getStatus() != null) {
                String statusName = job.getStatus().name();
                statusCounts.put(statusName, statusCounts.getOrDefault(statusName, 0L) + 1);
            }
        }

        // Brand Counts
        Map<String, Long> brandCounts = new HashMap<>();
        for (JobSheet job : jobs) {
            String brand = job.getBrand();
            if (brand != null && !brand.trim().isEmpty()) {
                String key = brand.trim().toUpperCase();
                brandCounts.put(key, brandCounts.getOrDefault(key, 0L) + 1);
            } else {
                brandCounts.put("UNKNOWN", brandCounts.getOrDefault("UNKNOWN", 0L) + 1);
            }
        }
        // Sort brandCounts descending by value
        Map<String, Long> sortedBrandCounts = brandCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Technician Performance
        Map<String, List<JobSheet>> techGroups = jobs.stream()
                .filter(job -> job.getTechnician() != null && !job.getTechnician().trim().isEmpty())
                .collect(Collectors.groupingBy(job -> job.getTechnician().trim()));

        List<TechnicianPerformanceDTO> technicianPerformance = new ArrayList<>();
        techGroups.forEach((tech, techJobs) -> {
            long jobCount = techJobs.size();
            BigDecimal totalEstimated = techJobs.stream()
                    .map(JobSheet::getEstimatedCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalFinal = techJobs.stream()
                    .map(JobSheet::getFinalCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            technicianPerformance.add(new TechnicianPerformanceDTO(tech, jobCount, totalEstimated, totalFinal));
        });

        // Sort by jobCount descending
        technicianPerformance.sort((t1, t2) -> Long.compare(t2.getJobCount(), t1.getJobCount()));

        return new JobSheetReportDTO(totalJobsCreated, sortedBrandCounts, sortedBrandCounts, technicianPerformance);
    }

    private InventoryReportDTO computeInventoryReport(LocalDate startDate, LocalDate endDate) {
        List<Product> products = productRepository.findAll();

        BigDecimal valuationPurchase = BigDecimal.ZERO;
        BigDecimal valuationSale = BigDecimal.ZERO;
        long lowStockCount = 0;
        List<Product> lowStockItems = new ArrayList<>();

        for (Product p : products) {
            int qty = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
            BigDecimal pPrice = p.getPurchasePrice() != null ? p.getPurchasePrice() : BigDecimal.ZERO;
            BigDecimal sPrice = p.getRate() != null ? p.getRate() : BigDecimal.ZERO;

            valuationPurchase = valuationPurchase.add(pPrice.multiply(BigDecimal.valueOf(qty)));
            valuationSale = valuationSale.add(sPrice.multiply(BigDecimal.valueOf(qty)));

            int reorder = p.getReorderLevel() != null ? p.getReorderLevel() : 0;
            if (qty <= reorder) {
                lowStockCount++;
                lowStockItems.add(p);
            }
        }

        // Top Selling Spares (computed from active sales invoices in date range)
        List<SalesInvoice> invoices = salesInvoiceRepository.findByInvoiceDateBetweenOrderByInvoiceDateDesc(startDate, endDate);
        Map<String, TopSellingHelper> productSalesMap = new HashMap<>();

        for (SalesInvoice si : invoices) {
            if (si.getStatus() == SalesInvoice.InvoiceStatus.CANCELLED) {
                continue;
            }
            if (si.getItems() != null) {
                for (SalesInvoiceItem item : si.getItems()) {
                    String name = item.getProduct() != null ? item.getProduct().getName() : item.getDescription();
                    if (name == null || name.trim().isEmpty()) {
                        name = "Service / Misc Charge";
                    }
                    name = name.trim();
                    int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                    BigDecimal total = item.getTotalAmount() != null ? item.getTotalAmount() : BigDecimal.ZERO;

                    TopSellingHelper helper = productSalesMap.getOrDefault(name, new TopSellingHelper(name, 0L, BigDecimal.ZERO));
                    helper.quantity += qty;
                    helper.revenue = helper.revenue.add(total);
                    productSalesMap.put(name, helper);
                }
            }
        }

        List<TopProductDTO> topSellingSpares = productSalesMap.values().stream()
                .map(h -> new TopProductDTO(h.name, h.quantity, h.revenue))
                .sorted((p1, p2) -> Long.compare(p2.getQuantitySold(), p1.getQuantitySold()))
                .limit(10)
                .collect(Collectors.toList());

        return new InventoryReportDTO(valuationPurchase, valuationSale, lowStockCount, lowStockItems, topSellingSpares);
    }

    private static class TopSellingHelper {
        String name;
        long quantity;
        BigDecimal revenue;

        TopSellingHelper(String name, long quantity, BigDecimal revenue) {
            this.name = name;
            this.quantity = quantity;
            this.revenue = revenue;
        }
    }
}
