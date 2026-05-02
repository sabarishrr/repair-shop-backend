package com.repairshop.controller;

import com.repairshop.model.Quotation;
import com.repairshop.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService service;

    @GetMapping
    public List<Quotation> getAll() {
        return service.getAllQuotations();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quotation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getQuotationById(id));
    }

    @PostMapping
    public Quotation create(@RequestBody Quotation quotation) {
        return service.saveQuotation(quotation);
    }

    @PutMapping("/{id}")
    public Quotation update(@PathVariable Long id, @RequestBody Quotation quotation) {
        quotation.setId(id);
        return service.saveQuotation(quotation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteQuotation(id);
        return ResponseEntity.ok().build();
    }
}
