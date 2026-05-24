package com.repairshop.service;

import com.repairshop.dto.CreditNoteItemRequest;
import com.repairshop.dto.CreditNoteRequest;
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
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final CustomerRepository customerRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final CompanyDetailsService companyDetailsService;
    private final TaxCalculationService taxCalculationService;

    public List<CreditNote> getAll() {
        return creditNoteRepository.findAllByOrderByNoteDateDesc();
    }

    public CreditNote getById(Long id) {
        return creditNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit Note not found: " + id));
    }

    @Transactional
    public CreditNote create(CreditNoteRequest req) {
        CreditNote creditNote = new CreditNote();

        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (req.getSalesInvoiceId() != null) {
            SalesInvoice salesInvoice = salesInvoiceRepository.findById(req.getSalesInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Sales Invoice not found"));
            creditNote.setSalesInvoice(salesInvoice);
        }

        CompanyDetails company = companyDetailsService.get();
        String finalNoteNum = req.getNoteNumber();
        if (finalNoteNum == null || finalNoteNum.trim().isEmpty()) {
            finalNoteNum = company.getCreditNotePrefix() + company.getNextCreditNoteNo();
            company.setNextCreditNoteNo(company.getNextCreditNoteNo() + 1);
            companyDetailsService.update(company);
        } else {
            company.setNextCreditNoteNo(company.getNextCreditNoteNo() + 1);
            companyDetailsService.update(company);
        }

        creditNote.setNoteNumber(finalNoteNum);
        creditNote.setCustomer(customer);
        creditNote.setNoteDate(req.getNoteDate());
        creditNote.setReason(req.getReason() != null ? req.getReason() : CreditNote.CreditNoteReason.OTHER);
        creditNote.setStatus(CreditNote.CreditNoteStatus.ACTIVE);
        creditNote.setNotes(req.getNotes());

        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;

        for (CreditNoteItemRequest itemReq : req.getItems()) {
            CreditNoteItem item = new CreditNoteItem();
            item.setCreditNote(creditNote);
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

                // Auto-reconcile stock if reason is SALES_RETURN (Increase stock)
                if (creditNote.getReason() == CreditNote.CreditNoteReason.SALES_RETURN) {
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);

                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setProduct(product);
                    tx.setType(InventoryTransaction.TransactionType.IN);
                    tx.setQuantity(item.getQuantity());
                    tx.setReferenceType("CREDIT_NOTE");
                    tx.setReferenceId(creditNote.getNoteNumber());
                    tx.setTransactionDate(LocalDateTime.now());
                    tx.setNotes("Sales return via credit note: " + creditNote.getNoteNumber());
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
                    taxableValue, gstPerc, customer.getState());

            item.setCgstAmount(taxes.getCgst());
            item.setSgstAmount(taxes.getSgst());
            item.setIgstAmount(taxes.getIgst());

            BigDecimal grandTotalLine = taxableValue.add(taxes.getTotalTax());
            item.setTotalAmount(grandTotalLine);

            creditNote.getItems().add(item);

            totalTaxable = totalTaxable.add(taxableValue);
            totalCgst = totalCgst.add(taxes.getCgst());
            totalSgst = totalSgst.add(taxes.getSgst());
            totalIgst = totalIgst.add(taxes.getIgst());
        }

        creditNote.setTotalTaxableValue(totalTaxable);
        creditNote.setTotalCgst(totalCgst);
        creditNote.setTotalSgst(totalSgst);
        creditNote.setTotalIgst(totalIgst);
        creditNote.setGrandTotal(totalTaxable.add(totalCgst).add(totalSgst).add(totalIgst));

        return creditNoteRepository.save(creditNote);
    }

    @Transactional
    public CreditNote cancel(Long id) {
        CreditNote creditNote = getById(id);
        if (creditNote.getStatus() == CreditNote.CreditNoteStatus.CANCELLED) {
            return creditNote;
        }

        // Reverse stock increase if SALES_RETURN (Decrease stock back)
        if (creditNote.getReason() == CreditNote.CreditNoteReason.SALES_RETURN) {
            for (CreditNoteItem item : creditNote.getItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                    productRepository.save(product);

                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setProduct(product);
                    tx.setType(InventoryTransaction.TransactionType.OUT);
                    tx.setQuantity(item.getQuantity());
                    tx.setReferenceType("CREDIT_NOTE_CANCELLED");
                    tx.setReferenceId(creditNote.getNoteNumber());
                    tx.setTransactionDate(LocalDateTime.now());
                    tx.setNotes("Credit note cancelled - stock reversed");
                    inventoryTransactionRepository.save(tx);
                }
            }
        }

        creditNote.setStatus(CreditNote.CreditNoteStatus.CANCELLED);
        return creditNoteRepository.save(creditNote);
    }

    @Transactional
    public void delete(Long id) {
        CreditNote creditNote = getById(id);
        
        // Reverse inventory if ACTIVE and SALES_RETURN
        if (creditNote.getStatus() == CreditNote.CreditNoteStatus.ACTIVE &&
            creditNote.getReason() == CreditNote.CreditNoteReason.SALES_RETURN) {
            for (CreditNoteItem item : creditNote.getItems()) {
                if (item.getProduct() != null) {
                    Product product = item.getProduct();
                    product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
                    productRepository.save(product);

                    InventoryTransaction tx = new InventoryTransaction();
                    tx.setProduct(product);
                    tx.setType(InventoryTransaction.TransactionType.OUT);
                    tx.setQuantity(item.getQuantity());
                    tx.setReferenceType("CREDIT_NOTE_DELETED");
                    tx.setReferenceId(creditNote.getNoteNumber());
                    tx.setTransactionDate(LocalDateTime.now());
                    tx.setNotes("Credit note deleted - stock reversed");
                    inventoryTransactionRepository.save(tx);
                }
            }
        }

        creditNoteRepository.delete(creditNote);
    }
}
