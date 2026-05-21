package com.repairshop.service;

import com.repairshop.dto.ReceiptRequest;
import com.repairshop.model.Customer;
import com.repairshop.model.Receipt;
import com.repairshop.model.SalesInvoice;
import com.repairshop.model.CompanyDetails;
import com.repairshop.repository.CustomerRepository;
import com.repairshop.repository.ReceiptRepository;
import com.repairshop.repository.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CompanyDetailsService companyDetailsService;

    public List<Receipt> getAll() {
        return receiptRepository.findAllByOrderByReceiptDateDesc();
    }

    public Receipt getById(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Receipt not found: " + id));
    }

    @Transactional
    public Receipt create(ReceiptRequest req) {
        Receipt receipt = new Receipt();
        
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        receipt.setCustomer(customer);
        
        if (req.getSalesInvoiceId() != null) {
            SalesInvoice invoice = salesInvoiceRepository.findById(req.getSalesInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            receipt.setSalesInvoice(invoice);
        }
        
        CompanyDetails company = companyDetailsService.get();
        String finalReceiptNumber = req.getReceiptNumber();
        if (finalReceiptNumber == null || finalReceiptNumber.trim().isEmpty() || finalReceiptNumber.startsWith("RCP-")) {
            finalReceiptNumber = company.getReceiptPrefix() + company.getNextReceiptNo();
            company.setNextReceiptNo(company.getNextReceiptNo() + 1);
            companyDetailsService.update(company);
        }
        receipt.setReceiptNumber(finalReceiptNumber);
        receipt.setReceiptDate(req.getReceiptDate() != null ? req.getReceiptDate() : LocalDate.now());
        receipt.setAmount(req.getAmount());
        receipt.setPaymentMethod(req.getPaymentMethod());
        receipt.setNotes(req.getNotes());
        
        return receiptRepository.save(receipt);
    }

    @Transactional
    public Receipt update(Long id, ReceiptRequest req) {
        Receipt receipt = getById(id);
        
        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        receipt.setCustomer(customer);
        
        if (req.getSalesInvoiceId() != null) {
            SalesInvoice invoice = salesInvoiceRepository.findById(req.getSalesInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            receipt.setSalesInvoice(invoice);
        } else {
            receipt.setSalesInvoice(null);
        }
        
        receipt.setReceiptDate(req.getReceiptDate());
        receipt.setAmount(req.getAmount());
        receipt.setPaymentMethod(req.getPaymentMethod());
        receipt.setNotes(req.getNotes());
        
        return receiptRepository.save(receipt);
    }

    @Transactional
    public void delete(Long id) {
        receiptRepository.deleteById(id);
    }
}
