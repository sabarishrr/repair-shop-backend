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
        return repository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    CompanyDetails defaults = new CompanyDetails();
                    defaults.setCompanyName("TechFix Pro");
                    defaults.setAddress("123 Repair Street, Tech City");
                    defaults.setPhone("+1 234 567 8900");
                    defaults.setEmail("support@techfix.pro");
                    defaults.setNextInvoiceNo(1L);
                    defaults.setNextReceiptNo(1L);
                    defaults.setNextPaymentNo(1L);
                    defaults.setInvoicePrefix("INV-");
                    defaults.setReceiptPrefix("REC-");
                    defaults.setPaymentPrefix("PAY-");
                    return repository.save(defaults);
                });
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
        
        existing.setNextInvoiceNo(request.getNextInvoiceNo());
        existing.setNextReceiptNo(request.getNextReceiptNo());
        existing.setNextPaymentNo(request.getNextPaymentNo());
        existing.setInvoicePrefix(request.getInvoicePrefix());
        existing.setReceiptPrefix(request.getReceiptPrefix());
        existing.setPaymentPrefix(request.getPaymentPrefix());
        
        return repository.save(existing);
    }
}
