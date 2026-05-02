package com.repairshop.service;

import com.repairshop.model.Quotation;
import com.repairshop.model.QuotationItem;
import com.repairshop.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository repository;

    public List<Quotation> getAllQuotations() {
        return repository.findAll();
    }

    public Quotation getQuotationById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Quotation not found"));
    }

    public Quotation saveQuotation(Quotation quotation) {
        calculateTotals(quotation);
        
        // Ensure bidirectional relationship is set before saving
        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                item.setQuotation(quotation);
            }
        }
        
        return repository.save(quotation);
    }

    public void deleteQuotation(Long id) {
        repository.deleteById(id);
    }

    private void calculateTotals(Quotation quotation) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                // Rate * Qty
                BigDecimal lineSubTotal = item.getRate().multiply(BigDecimal.valueOf(item.getQuantity()));
                
                // (Rate * Qty * GST%) / 100
                BigDecimal gstAmount = lineSubTotal
                        .multiply(item.getGstPercentage())
                        .divide(BigDecimal.valueOf(100));
                        
                BigDecimal totalAmount = lineSubTotal.add(gstAmount);

                item.setGstAmount(gstAmount);
                item.setTotalAmount(totalAmount);

                subTotal = subTotal.add(lineSubTotal);
                taxTotal = taxTotal.add(gstAmount);
            }
        }

        quotation.setSubTotal(subTotal);
        quotation.setTaxTotal(taxTotal);
        quotation.setGrandTotal(subTotal.add(taxTotal));
    }
}
