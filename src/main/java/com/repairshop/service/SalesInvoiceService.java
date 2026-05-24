package com.repairshop.service;

import com.repairshop.dto.SalesInvoiceRequest;
import com.repairshop.dto.SalesItemRequest;
import com.repairshop.model.*;
import com.repairshop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesInvoiceService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final TaxCalculationService taxCalculationService;
    private final ReceiptRepository receiptRepository;
    private final CompanyDetailsService companyDetailsService;

    public List<SalesInvoice> getAll() {
        return salesInvoiceRepository.findAllByOrderByInvoiceDateDesc();
    }

    public SalesInvoice getById(Long id) {
        return salesInvoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sales Invoice not found: " + id));
    }

    @Transactional
    public SalesInvoice create(SalesInvoiceRequest req) {
        SalesInvoice invoice = new SalesInvoice();
        
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        CompanyDetails company = companyDetailsService.get();
        String finalInvNum = req.getInvoiceNumber();
        if (finalInvNum == null || finalInvNum.trim().isEmpty()) {
            finalInvNum = company.getInvoicePrefix() + company.getNextInvoiceNo();
            company.setNextInvoiceNo(company.getNextInvoiceNo() + 1);
            companyDetailsService.update(company);
        } else {
            // Frontend provided the invoice number (assembled from nextInvoiceNo).
            // Still increment the counter so the next invoice gets a new number.
            company.setNextInvoiceNo(company.getNextInvoiceNo() + 1);
            companyDetailsService.update(company);
        }
        invoice.setInvoiceNumber(finalInvNum);
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(req.getInvoiceDate());
        invoice.setNotes(req.getNotes());
        invoice.setStatus(req.getStatus() != null ? req.getStatus() : SalesInvoice.InvoiceStatus.PAID);
        invoice.setSalesType(req.getSalesType() != null ? req.getSalesType() : "CASH");
        invoice.setPaymentMethod(req.getPaymentMethod());
        invoice.setReceivedAmount(req.getReceivedAmount() != null ? req.getReceivedAmount() : BigDecimal.ZERO);
        
        invoice.setDeliveryNote(req.getDeliveryNote());
        invoice.setPaymentTerms(req.getPaymentTerms());
        invoice.setSupplierRef(req.getSupplierRef());
        invoice.setBuyerOrderNo(req.getBuyerOrderNo());
        invoice.setBuyerOrderDate(req.getBuyerOrderDate());
        invoice.setDespatchDocumentNo(req.getDespatchDocumentNo());
        invoice.setDeliveryNoteDate(req.getDeliveryNoteDate());
        invoice.setDespatchedThrough(req.getDespatchedThrough());
        invoice.setDestination(req.getDestination());
        invoice.setTermsOfDelivery(req.getTermsOfDelivery());

        if (req.getQuotationId() != null) {
            Quotation quotation = quotationRepository.findById(req.getQuotationId())
                    .orElseThrow(() -> new RuntimeException("Quotation not found"));
            invoice.setQuotation(quotation);
        }

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (SalesItemRequest itemReq : req.getItems()) {
            SalesInvoiceItem item = new SalesInvoiceItem();
            item.setSalesInvoice(invoice);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO);

            BigDecimal gstPerc = itemReq.getGstPercentage() != null ? itemReq.getGstPercentage() : BigDecimal.ZERO;

            if (itemReq.getProductId() != null) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                item.setProduct(product);
                item.setDescription(product.getName());
                item.setHsn(product.getHsn());
                gstPerc = product.getGstPercentage();

                // Handle Inventory Update
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                productRepository.save(product);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setProduct(product);
                tx.setType(InventoryTransaction.TransactionType.OUT);
                tx.setQuantity(item.getQuantity());
                tx.setReferenceType("SALES_INVOICE");
                tx.setReferenceId(invoice.getInvoiceNumber());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setNotes("Sales stock outward");
                inventoryTransactionRepository.save(tx);
            } else {
                item.setDescription(itemReq.getDescription());
                item.setHsn(itemReq.getHsn());
            }
            item.setSerialNumber(itemReq.getSerialNumber());
            item.setWarrantyPeriod(itemReq.getWarrantyPeriod());

            item.setGstPercentage(gstPerc);

            // Calculate item values
            BigDecimal lineTotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            BigDecimal taxableValue = lineTotal.subtract(item.getDiscount());
            item.setTaxableValue(taxableValue);

            TaxCalculationService.TaxResult taxes = taxCalculationService.calculateTax(
                    taxableValue, gstPerc, customer.getState());

            item.setCgstAmount(taxes.getCgst());
            item.setSgstAmount(taxes.getSgst());
            item.setIgstAmount(taxes.getIgst());
            
            BigDecimal grandTotalLine = taxableValue.add(taxes.getTotalTax());
            item.setTotalAmount(grandTotalLine);

            invoice.getItems().add(item);

            // Add to invoice totals
            totalTaxable = totalTaxable.add(taxableValue);
            totalCgst = totalCgst.add(taxes.getCgst());
            totalSgst = totalSgst.add(taxes.getSgst());
            totalIgst = totalIgst.add(taxes.getIgst());
        }

        invoice.setTotalTaxableValue(totalTaxable);
        invoice.setTotalCgst(totalCgst);
        invoice.setTotalSgst(totalSgst);
        invoice.setTotalIgst(totalIgst);
        invoice.setGrandTotal(totalTaxable.add(totalCgst).add(totalSgst).add(totalIgst));

        SalesInvoice savedInvoice = salesInvoiceRepository.save(invoice);
        handleReceiptGeneration(savedInvoice);
        return savedInvoice;
    }

    @Transactional
    public SalesInvoice update(Long id, SalesInvoiceRequest req) {
        SalesInvoice invoice = getById(id);

        // 1. Reverse existing inventory
        for (SalesInvoiceItem item : invoice.getItems()) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setProduct(product);
                tx.setType(InventoryTransaction.TransactionType.IN);
                tx.setQuantity(item.getQuantity());
                tx.setReferenceType("SALES_UPDATED");
                tx.setReferenceId(invoice.getInvoiceNumber());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setNotes("Sales invoice edited - stock reversed");
                inventoryTransactionRepository.save(tx);
            }
        }

        // 2. Clear old items
        invoice.getItems().clear();

        // 3. Update headers
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(req.getInvoiceDate());
        invoice.setNotes(req.getNotes());
        invoice.setStatus(req.getStatus() != null ? req.getStatus() : SalesInvoice.InvoiceStatus.PAID);
        invoice.setSalesType(req.getSalesType() != null ? req.getSalesType() : "CASH");
        invoice.setPaymentMethod(req.getPaymentMethod());
        invoice.setReceivedAmount(req.getReceivedAmount() != null ? req.getReceivedAmount() : BigDecimal.ZERO);

        invoice.setDeliveryNote(req.getDeliveryNote());
        invoice.setPaymentTerms(req.getPaymentTerms());
        invoice.setSupplierRef(req.getSupplierRef());
        invoice.setBuyerOrderNo(req.getBuyerOrderNo());
        invoice.setBuyerOrderDate(req.getBuyerOrderDate());
        invoice.setDespatchDocumentNo(req.getDespatchDocumentNo());
        invoice.setDeliveryNoteDate(req.getDeliveryNoteDate());
        invoice.setDespatchedThrough(req.getDespatchedThrough());
        invoice.setDestination(req.getDestination());
        invoice.setTermsOfDelivery(req.getTermsOfDelivery());

        if (req.getQuotationId() != null) {
            Quotation quotation = quotationRepository.findById(req.getQuotationId())
                    .orElseThrow(() -> new RuntimeException("Quotation not found"));
            invoice.setQuotation(quotation);
        } else {
            invoice.setQuotation(null);
        }

        // 4. Process new items
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (SalesItemRequest itemReq : req.getItems()) {
            SalesInvoiceItem item = new SalesInvoiceItem();
            item.setSalesInvoice(invoice);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO);

            BigDecimal gstPerc = itemReq.getGstPercentage() != null ? itemReq.getGstPercentage() : BigDecimal.ZERO;

            if (itemReq.getProductId() != null) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                item.setProduct(product);
                item.setDescription(product.getName());
                item.setHsn(product.getHsn());
                gstPerc = product.getGstPercentage();

                // Handle Inventory Update
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                productRepository.save(product);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setProduct(product);
                tx.setType(InventoryTransaction.TransactionType.OUT);
                tx.setQuantity(item.getQuantity());
                tx.setReferenceType("SALES_INVOICE");
                tx.setReferenceId(invoice.getInvoiceNumber());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setNotes("Sales stock outward");
                inventoryTransactionRepository.save(tx);
            } else {
                item.setDescription(itemReq.getDescription());
                item.setHsn(itemReq.getHsn());
            }
            item.setSerialNumber(itemReq.getSerialNumber());
            item.setWarrantyPeriod(itemReq.getWarrantyPeriod());

            item.setGstPercentage(gstPerc);

            // Calculate item values
            BigDecimal lineTotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            BigDecimal taxableValue = lineTotal.subtract(item.getDiscount());
            item.setTaxableValue(taxableValue);

            TaxCalculationService.TaxResult taxes = taxCalculationService.calculateTax(
                    taxableValue, gstPerc, customer.getState());

            item.setCgstAmount(taxes.getCgst());
            item.setSgstAmount(taxes.getSgst());
            item.setIgstAmount(taxes.getIgst());
            
            BigDecimal grandTotalLine = taxableValue.add(taxes.getTotalTax());
            item.setTotalAmount(grandTotalLine);

            invoice.getItems().add(item);

            // Add to invoice totals
            totalTaxable = totalTaxable.add(taxableValue);
            totalCgst = totalCgst.add(taxes.getCgst());
            totalSgst = totalSgst.add(taxes.getSgst());
            totalIgst = totalIgst.add(taxes.getIgst());
        }

        invoice.setTotalTaxableValue(totalTaxable);
        invoice.setTotalCgst(totalCgst);
        invoice.setTotalSgst(totalSgst);
        invoice.setTotalIgst(totalIgst);
        invoice.setGrandTotal(totalTaxable.add(totalCgst).add(totalSgst).add(totalIgst));

        SalesInvoice savedInvoice = salesInvoiceRepository.save(invoice);
        handleReceiptGeneration(savedInvoice);
        return savedInvoice;
    }

    @Transactional
    public void delete(Long id) {
        SalesInvoice invoice = getById(id);
        
        // Delete linked receipts first
        List<Receipt> existingReceipts = receiptRepository.findBySalesInvoiceId(invoice.getId());
        if (!existingReceipts.isEmpty()) {
            receiptRepository.deleteAll(existingReceipts);
        }
        
        // Reverse inventory if products were involved
        for (SalesInvoiceItem item : invoice.getItems()) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setProduct(product);
                tx.setType(InventoryTransaction.TransactionType.IN);
                tx.setQuantity(item.getQuantity());
                tx.setReferenceType("SALES_CANCELLED");
                tx.setReferenceId(invoice.getInvoiceNumber());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setNotes("Sales invoice deleted/reversed");
                inventoryTransactionRepository.save(tx);
            }
        }
        
        salesInvoiceRepository.deleteById(id);
    }

    private void handleReceiptGeneration(SalesInvoice invoice) {
        if (invoice.getReceivedAmount() != null && invoice.getReceivedAmount().compareTo(BigDecimal.ZERO) > 0) {
            List<Receipt> existingReceipts = receiptRepository.findBySalesInvoiceId(invoice.getId());
            Receipt receipt;
            if (!existingReceipts.isEmpty()) {
                receipt = existingReceipts.get(0);
            } else {
                receipt = new Receipt();
                CompanyDetails company = companyDetailsService.get();
                receipt.setReceiptNumber(company.getReceiptPrefix() + company.getNextReceiptNo());
                company.setNextReceiptNo(company.getNextReceiptNo() + 1);
                companyDetailsService.update(company);
            }
            receipt.setCustomer(invoice.getCustomer());
            receipt.setSalesInvoice(invoice);
            receipt.setReceiptDate(invoice.getInvoiceDate());
            receipt.setAmount(invoice.getReceivedAmount());
            receipt.setPaymentMethod(invoice.getPaymentMethod() != null ? invoice.getPaymentMethod() : "CASH");
            receipt.setNotes("Automatically generated from invoice: " + invoice.getInvoiceNumber());
            receiptRepository.save(receipt);
        } else {
            List<Receipt> existingReceipts = receiptRepository.findBySalesInvoiceId(invoice.getId());
            if (!existingReceipts.isEmpty()) {
                receiptRepository.deleteAll(existingReceipts);
            }
        }
    }
}
