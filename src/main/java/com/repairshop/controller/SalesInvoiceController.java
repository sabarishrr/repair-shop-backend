package com.repairshop.controller;

import com.repairshop.dto.SalesInvoiceRequest;
import com.repairshop.model.SalesInvoice;
import com.repairshop.service.SalesInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class SalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;

    @GetMapping
    public List<SalesInvoice> getAll() {
        return salesInvoiceService.getAll();
    }

    @GetMapping("/{id}")
    public SalesInvoice getById(@PathVariable Long id) {
        return salesInvoiceService.getById(id);
    }

    @PostMapping
    public SalesInvoice create(@RequestBody SalesInvoiceRequest request) {
        return salesInvoiceService.create(request);
    }

    @PutMapping("/{id}")
    public SalesInvoice update(@PathVariable Long id, @RequestBody SalesInvoiceRequest request) {
        return salesInvoiceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesInvoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
