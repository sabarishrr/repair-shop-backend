package com.repairshop.controller;

import com.repairshop.model.CompanyDetails;
import com.repairshop.service.CompanyDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyDetailsController {

    private final CompanyDetailsService companyDetailsService;

    /**
     * Get company details — accessible by any authenticated user
     * (needed for print headers, etc.)
     */
    @GetMapping
    public CompanyDetails get() {
        return companyDetailsService.get();
    }

    /**
     * Update company details — admin only
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CompanyDetails update(@RequestBody CompanyDetails request) {
        return companyDetailsService.update(request);
    }
}
