package com.repairshop.service;

import com.repairshop.dto.PurchaseInvoiceRequest;
import com.repairshop.dto.PurchaseItemRequest;
import com.repairshop.model.*;
import com.repairshop.repository.InventoryTransactionRepository;
import com.repairshop.repository.ProductRepository;
import com.repairshop.repository.PurchaseInvoiceRepository;
import com.repairshop.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseInvoiceService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final TaxCalculationService taxCalculationService;

    public List<PurchaseInvoice> getAll() {
        return purchaseInvoiceRepository.findAllByOrderByInvoiceDateDesc();
    }

    public PurchaseInvoice getById(Long id) {
        return purchaseInvoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase Invoice not found: " + id));
    }

    @Transactional
    public PurchaseInvoice create(PurchaseInvoiceRequest req) {
        PurchaseInvoice invoice = new PurchaseInvoice();
        
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        
        invoice.setInvoiceNumber(req.getInvoiceNumber());
        invoice.setSupplier(supplier);
        invoice.setInvoiceDate(req.getInvoiceDate());
        invoice.setNotes(req.getNotes());
        invoice.setStatus(req.getStatus() != null ? req.getStatus() : PurchaseInvoice.PurchaseStatus.RECEIVED);

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (PurchaseItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            PurchaseItem item = new PurchaseItem();
            item.setPurchaseInvoice(invoice);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setRate(itemReq.getRate());
            item.setDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO);

            // Calculate item values
            BigDecimal lineTotal = item.getRate().multiply(new BigDecimal(item.getQuantity()));
            BigDecimal taxableValue = lineTotal.subtract(item.getDiscount());
            item.setTaxableValue(taxableValue);

            TaxCalculationService.TaxResult taxes = taxCalculationService.calculateTax(
                    taxableValue, product.getGstPercentage(), supplier.getState());

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

            // Handle Inventory Update if status is RECEIVED
            if (invoice.getStatus() == PurchaseInvoice.PurchaseStatus.RECEIVED) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setProduct(product);
                tx.setType(InventoryTransaction.TransactionType.IN);
                tx.setQuantity(item.getQuantity());
                tx.setReferenceType("PURCHASE_INVOICE");
                tx.setReferenceId(invoice.getInvoiceNumber());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setNotes("Purchase stock inward");
                inventoryTransactionRepository.save(tx);
            }
        }

        invoice.setTotalTaxableValue(totalTaxable);
        invoice.setTotalCgst(totalCgst);
        invoice.setTotalSgst(totalSgst);
        invoice.setTotalIgst(totalIgst);
        invoice.setGrandTotal(totalTaxable.add(totalCgst).add(totalSgst).add(totalIgst));

        return purchaseInvoiceRepository.save(invoice);
    }

    @Transactional
    public PurchaseInvoice update(Long id, PurchaseInvoiceRequest req) {
        PurchaseInvoice invoice = getById(id);

        // 1. Reverse inventory if it was received
        if (invoice.getStatus() == PurchaseInvoice.PurchaseStatus.RECEIVED) {
            for (PurchaseItem item : invoice.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                productRepository.save(product);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setProduct(product);
                tx.setType(InventoryTransaction.TransactionType.OUT);
                tx.setQuantity(item.getQuantity());
                tx.setReferenceType("PURCHASE_UPDATED");
                tx.setReferenceId(invoice.getInvoiceNumber());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setNotes("Purchase invoice edited - stock reversed");
                inventoryTransactionRepository.save(tx);
            }
        }

        // 2. Clear old items
        invoice.getItems().clear();

        // 3. Update headers
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        
        invoice.setSupplier(supplier);
        invoice.setInvoiceDate(req.getInvoiceDate());
        invoice.setNotes(req.getNotes());
        invoice.setStatus(req.getStatus() != null ? req.getStatus() : PurchaseInvoice.PurchaseStatus.RECEIVED);

        // 4. Process new items
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (PurchaseItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            PurchaseItem item = new PurchaseItem();
            item.setPurchaseInvoice(invoice);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setRate(itemReq.getRate());
            item.setDiscount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO);

            // Calculate item values
            BigDecimal lineTotal = item.getRate().multiply(new BigDecimal(item.getQuantity()));
            BigDecimal taxableValue = lineTotal.subtract(item.getDiscount());
            item.setTaxableValue(taxableValue);

            TaxCalculationService.TaxResult taxes = taxCalculationService.calculateTax(
                    taxableValue, product.getGstPercentage(), supplier.getState());

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

            // Handle Inventory Update if status is RECEIVED
            if (invoice.getStatus() == PurchaseInvoice.PurchaseStatus.RECEIVED) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setProduct(product);
                tx.setType(InventoryTransaction.TransactionType.IN);
                tx.setQuantity(item.getQuantity());
                tx.setReferenceType("PURCHASE_INVOICE");
                tx.setReferenceId(invoice.getInvoiceNumber());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setNotes("Purchase stock inward");
                inventoryTransactionRepository.save(tx);
            }
        }

        invoice.setTotalTaxableValue(totalTaxable);
        invoice.setTotalCgst(totalCgst);
        invoice.setTotalSgst(totalSgst);
        invoice.setTotalIgst(totalIgst);
        invoice.setGrandTotal(totalTaxable.add(totalCgst).add(totalSgst).add(totalIgst));

        return purchaseInvoiceRepository.save(invoice);
    }

    @Transactional
    public void delete(Long id) {
        PurchaseInvoice invoice = getById(id);
        
        // Reverse inventory if it was received
        if (invoice.getStatus() == PurchaseInvoice.PurchaseStatus.RECEIVED) {
            for (PurchaseItem item : invoice.getItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                productRepository.save(product);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setProduct(product);
                tx.setType(InventoryTransaction.TransactionType.OUT);
                tx.setQuantity(item.getQuantity());
                tx.setReferenceType("PURCHASE_CANCELLED");
                tx.setReferenceId(invoice.getInvoiceNumber());
                tx.setTransactionDate(LocalDateTime.now());
                tx.setNotes("Purchase invoice deleted/reversed");
                inventoryTransactionRepository.save(tx);
            }
        }
        
        purchaseInvoiceRepository.deleteById(id);
    }
}
