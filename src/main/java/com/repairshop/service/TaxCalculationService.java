package com.repairshop.service;

import com.repairshop.model.CompanyDetails;
import com.repairshop.model.State;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class TaxCalculationService {

    private final CompanyDetailsService companyDetailsService;

    public TaxResult calculateTax(BigDecimal taxableValue, BigDecimal gstPercentage, State targetState) {
        TaxResult result = new TaxResult();
        if (taxableValue == null || gstPercentage == null || gstPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return result;
        }

        CompanyDetails company = companyDetailsService.get();
        boolean isInterState = true;
        
        if (company != null && company.getState() != null && targetState != null) {
            if (company.getState().getId().equals(targetState.getId())) {
                isInterState = false;
            }
        }

        BigDecimal totalTaxAmount = taxableValue.multiply(gstPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        if (isInterState) {
            result.setIgst(totalTaxAmount);
        } else {
            BigDecimal halfTax = totalTaxAmount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            result.setCgst(halfTax);
            result.setSgst(totalTaxAmount.subtract(halfTax)); // Subtract to handle odd cent splits safely
        }

        return result;
    }

    public static class TaxResult {
        private BigDecimal cgst = BigDecimal.ZERO;
        private BigDecimal sgst = BigDecimal.ZERO;
        private BigDecimal igst = BigDecimal.ZERO;

        public BigDecimal getCgst() { return cgst; }
        public void setCgst(BigDecimal cgst) { this.cgst = cgst; }

        public BigDecimal getSgst() { return sgst; }
        public void setSgst(BigDecimal sgst) { this.sgst = sgst; }

        public BigDecimal getIgst() { return igst; }
        public void setIgst(BigDecimal igst) { this.igst = igst; }
        
        public BigDecimal getTotalTax() {
            return cgst.add(sgst).add(igst);
        }
    }
}
