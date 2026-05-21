package com.repairshop.service;

import com.repairshop.dto.PaymentRequest;
import com.repairshop.model.Supplier;
import com.repairshop.model.Payment;
import com.repairshop.model.PurchaseInvoice;
import com.repairshop.model.CompanyDetails;
import com.repairshop.repository.SupplierRepository;
import com.repairshop.repository.PaymentRepository;
import com.repairshop.repository.PurchaseInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final CompanyDetailsService companyDetailsService;

    public List<Payment> getAll() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }

    public Payment getById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + id));
    }

    @Transactional
    public Payment create(PaymentRequest req) {
        Payment payment = new Payment();
        
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        payment.setSupplier(supplier);
        
        if (req.getPurchaseInvoiceId() != null) {
            PurchaseInvoice invoice = purchaseInvoiceRepository.findById(req.getPurchaseInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            payment.setPurchaseInvoice(invoice);
        }
        
        CompanyDetails company = companyDetailsService.get();
        String finalPaymentNumber = req.getPaymentNumber();
        if (finalPaymentNumber == null || finalPaymentNumber.trim().isEmpty() || finalPaymentNumber.startsWith("PMT-")) {
            finalPaymentNumber = company.getPaymentPrefix() + company.getNextPaymentNo();
            company.setNextPaymentNo(company.getNextPaymentNo() + 1);
            companyDetailsService.update(company);
        }
        payment.setPaymentNumber(finalPaymentNumber);
        payment.setPaymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now());
        payment.setAmount(req.getAmount());
        payment.setPaymentMethod(req.getPaymentMethod());
        payment.setNotes(req.getNotes());
        
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment update(Long id, PaymentRequest req) {
        Payment payment = getById(id);
        
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        payment.setSupplier(supplier);
        
        if (req.getPurchaseInvoiceId() != null) {
            PurchaseInvoice invoice = purchaseInvoiceRepository.findById(req.getPurchaseInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            payment.setPurchaseInvoice(invoice);
        } else {
            payment.setPurchaseInvoice(null);
        }
        
        payment.setPaymentDate(req.getPaymentDate());
        payment.setAmount(req.getAmount());
        payment.setPaymentMethod(req.getPaymentMethod());
        payment.setNotes(req.getNotes());
        
        return paymentRepository.save(payment);
    }

    @Transactional
    public void delete(Long id) {
        paymentRepository.deleteById(id);
    }
}
