package com.repairshop.service;

import com.repairshop.model.CompanyDetails;
import com.repairshop.repository.CompanyDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyDetailsService {

    private final CompanyDetailsRepository repository;

    /**
     * Get company details. Returns the single row, or creates a default one if none exists.
     */
    public CompanyDetails get() {
        CompanyDetails details = repository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    CompanyDetails defaults = new CompanyDetails();
                    defaults.setCompanyName("TechFix Pro");
                    defaults.setAddress("123 Repair Street, Tech City");
                    defaults.setPhone("+1 234 567 8900");
                    defaults.setEmail("support@techfix.pro");
                    defaults.setNextInvoiceNo(1L);
                    defaults.setNextReceiptNo(1L);
                    defaults.setNextPaymentNo(1L);
                    defaults.setNextCreditNoteNo(1L);
                    defaults.setNextDebitNoteNo(1L);
                    defaults.setInvoicePrefix("INV-");
                    defaults.setReceiptPrefix("REC-");
                    defaults.setPaymentPrefix("PAY-");
                    defaults.setCreditNotePrefix("CN-");
                    defaults.setDebitNotePrefix("DN-");
                    return repository.save(defaults);
                });

        boolean updated = false;
        if (details.getNextCreditNoteNo() == null) {
            details.setNextCreditNoteNo(1L);
            updated = true;
        }
        if (details.getNextDebitNoteNo() == null) {
            details.setNextDebitNoteNo(1L);
            updated = true;
        }
        if (details.getCreditNotePrefix() == null) {
            details.setCreditNotePrefix("CN-");
            updated = true;
        }
        if (details.getDebitNotePrefix() == null) {
            details.setDebitNotePrefix("DN-");
            updated = true;
        }
        if (details.getNextInvoiceNo() == null) {
            details.setNextInvoiceNo(1L);
            updated = true;
        }
        if (details.getNextReceiptNo() == null) {
            details.setNextReceiptNo(1L);
            updated = true;
        }
        if (details.getNextPaymentNo() == null) {
            details.setNextPaymentNo(1L);
            updated = true;
        }
        if (details.getInvoicePrefix() == null) {
            details.setInvoicePrefix("INV-");
            updated = true;
        }
        if (details.getReceiptPrefix() == null) {
            details.setReceiptPrefix("REC-");
            updated = true;
        }
        if (details.getPaymentPrefix() == null) {
            details.setPaymentPrefix("PAY-");
            updated = true;
        }

        if (updated) {
            details = repository.save(details);
        }

        return details;
    }

    /**
     * Update company details. Only one row exists — update it.
     */
    public CompanyDetails update(CompanyDetails request) {
        CompanyDetails existing = get();
        existing.setCompanyName(request.getCompanyName());
        existing.setAddress(request.getAddress());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setLogoUrl(request.getLogoUrl());
        existing.setGstNumber(request.getGstNumber());
        existing.setState(request.getState());
        existing.setBankName(request.getBankName());
        existing.setAccountNumber(request.getAccountNumber());
        existing.setBranchIfsCode(request.getBranchIfsCode());
        existing.setUpiId(request.getUpiId());
        
        existing.setNextInvoiceNo(request.getNextInvoiceNo());
        existing.setNextReceiptNo(request.getNextReceiptNo());
        existing.setNextPaymentNo(request.getNextPaymentNo());
        existing.setNextCreditNoteNo(request.getNextCreditNoteNo());
        existing.setNextDebitNoteNo(request.getNextDebitNoteNo());
        existing.setInvoicePrefix(request.getInvoicePrefix());
        existing.setReceiptPrefix(request.getReceiptPrefix());
        existing.setPaymentPrefix(request.getPaymentPrefix());
        existing.setCreditNotePrefix(request.getCreditNotePrefix());
        existing.setDebitNotePrefix(request.getDebitNotePrefix());
        
        return repository.save(existing);
    }
}
