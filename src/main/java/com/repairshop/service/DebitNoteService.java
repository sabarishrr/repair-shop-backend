package com.repairshop.service;

import com.repairshop.dto.DebitNoteItemRequest;
import com.repairshop.dto.DebitNoteRequest;
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
public class DebitNoteService {

    private final DebitNoteRepository debitNoteRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final CompanyDetailsService companyDetailsService;
    private final TaxCalculationService taxCalculationService;

    public List<DebitNote> getAll() {
        return debitNoteRepository.findAllByOrderByNoteDateDesc();
    }

    public DebitNote getById(Long id) {
        return debitNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Debit Note not found: " + id));
    }

    @Transactional
    public DebitNote create(DebitNoteRequest req) {
        DebitNote debitNote = new DebitNote();

        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        if (req.getPurchaseInvoiceId() != null) {
            PurchaseInvoice purchaseInvoice = purchaseInvoiceRepository.findById(req.getPurchaseInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Purchase Invoice not found"));
            debitNote.setPurchaseInvoice(purchaseInvoice);
        }

        CompanyDetails company = companyDetailsService.get();
        String finalNoteNum = req.getNoteNumber();
        if (finalNoteNum == null || finalNoteNum.trim().isEmpty()) {
            finalNoteNum = company.getDebitNotePrefix() + company.getNextDebitNoteNo();
            company.setNextDebitNoteNo(company.getNextDebitNoteNo() + 1);
            companyDetailsService.update(company);
        } else {
            company.setNextDebitNoteNo(company.getNextDebitNoteNo() + 1);
            companyDetailsService.update(company);
        }

        debitNote.setNoteNumber(finalNoteNum);
        debitNote.setSupplier(supplier);
        debitNote.setNoteDate(req.getNoteDate());
        debitNote.setReason(req.getReason() != null ? req.getReason() : DebitNote.DebitNoteReason.OTHER);
        debitNote.setStatus(DebitNote.DebitNoteStatus.ACTIVE);
        debitNote.setNotes(req.getNotes());

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (DebitNoteItemRequest itemReq : req.getItems()) {
            DebitNoteItem item = new DebitNoteItem();
            item.setDebitNote(debitNote);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());

            BigDecimal gstPerc = itemReq.getGstPercentage() != null ? itemReq.getGstPercentage() : BigDecimal.ZERO;

            if (itemReq.getProductId() != null) {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found"));
                item.setProduct(product);
                item.setDescription(product.getName());
                item.setHsn(product.getHsn());
                gstPerc = product.getGstPercentage();

                // Auto-reconcile stock if reason is PURCHASE_RETURN (Decrease stock)
                if (debitNote.getReason() == DebitNote.DebitNoteReason.PURCHASE_RETURN) {
                    product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                    productRepository.save(product);

                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setProduct(product);
                    tx.setType(InventoryTransaction.TransactionType.OUT);
                    tx.setQuantity(item.getQuantity());
                    tx.setReferenceType("DEBIT_NOTE");
                    tx.setReferenceId(debitNote.getNoteNumber());
                    tx.setTransactionDate(LocalDateTime.now());
                    tx.setNotes("Purchase return via debit note: " + debitNote.getNoteNumber());
                    inventoryTransactionRepository.save(tx);
                }
            } else {
                item.setDescription(itemReq.getDescription());
                item.setHsn(itemReq.getHsn());
            }

            item.setGstPercentage(gstPerc);

            // Calculate item values
            BigDecimal taxableValue = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            item.setTaxableValue(taxableValue);

            TaxCalculationService.TaxResult taxes = taxCalculationService.calculateTax(
                    taxableValue, gstPerc, supplier.getState());

            item.setCgstAmount(taxes.getCgst());
            item.setSgstAmount(taxes.getSgst());
            item.setIgstAmount(taxes.getIgst());

            BigDecimal grandTotalLine = taxableValue.add(taxes.getTotalTax());
            item.setTotalAmount(grandTotalLine);

            debitNote.getItems().add(item);

            totalTaxable = totalTaxable.add(taxableValue);
            totalCgst = totalCgst.add(taxes.getCgst());
            totalSgst = totalSgst.add(taxes.getSgst());
            totalIgst = totalIgst.add(taxes.getIgst());
        }

        debitNote.setTotalTaxableValue(totalTaxable);
        debitNote.setTotalCgst(totalCgst);
        debitNote.setTotalSgst(totalSgst);
        debitNote.setTotalIgst(totalIgst);
        debitNote.setGrandTotal(totalTaxable.add(totalCgst).add(totalSgst).add(totalIgst));

        return debitNoteRepository.save(debitNote);
    }

    @Transactional
    public DebitNote cancel(Long id) {
        DebitNote debitNote = getById(id);
        if (debitNote.getStatus() == DebitNote.DebitNoteStatus.CANCELLED) {
            return debitNote;
        }

        // Reverse stock decrease if PURCHASE_RETURN (Increase stock back)
        if (debitNote.getReason() == DebitNote.DebitNoteReason.PURCHASE_RETURN) {
            for (DebitNoteItem item : debitNote.getItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);

                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setProduct(product);
                    tx.setType(InventoryTransaction.TransactionType.IN);
                    tx.setQuantity(item.getQuantity());
                    tx.setReferenceType("DEBIT_NOTE_CANCELLED");
                    tx.setReferenceId(debitNote.getNoteNumber());
                    tx.setTransactionDate(LocalDateTime.now());
                    tx.setNotes("Debit note cancelled - stock reversed");
                    inventoryTransactionRepository.save(tx);
                }
            }
        }

        debitNote.setStatus(DebitNote.DebitNoteStatus.CANCELLED);
        return debitNoteRepository.save(debitNote);
    }

    @Transactional
    public void delete(Long id) {
        DebitNote debitNote = getById(id);

        // Reverse inventory if ACTIVE and PURCHASE_RETURN
        if (debitNote.getStatus() == DebitNote.DebitNoteStatus.ACTIVE &&
            debitNote.getReason() == DebitNote.DebitNoteReason.PURCHASE_RETURN) {
            for (DebitNoteItem item : debitNote.getItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);

                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setProduct(product);
                    tx.setType(InventoryTransaction.TransactionType.IN);
                    tx.setQuantity(item.getQuantity());
                    tx.setReferenceType("DEBIT_NOTE_DELETED");
                    tx.setReferenceId(debitNote.getNoteNumber());
                    tx.setTransactionDate(LocalDateTime.now());
                    tx.setNotes("Debit note deleted - stock reversed");
                    inventoryTransactionRepository.save(tx);
                }
            }
        }

        debitNoteRepository.delete(debitNote);
    }
}
