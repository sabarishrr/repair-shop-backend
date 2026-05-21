package com.repairshop.controller;

import com.repairshop.dto.PurchaseInvoiceRequest;
import com.repairshop.model.PurchaseInvoice;
import com.repairshop.service.PurchaseInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService purchaseInvoiceService;

    @GetMapping
    public List<PurchaseInvoice> getAll() {
        return purchaseInvoiceService.getAll();
    }

    @GetMapping("/{id}")
    public PurchaseInvoice getById(@PathVariable Long id) {
        return purchaseInvoiceService.getById(id);
    }

    @PostMapping
    public PurchaseInvoice create(@RequestBody PurchaseInvoiceRequest request) {
        return purchaseInvoiceService.create(request);
    }

    @PutMapping("/{id}")
    public PurchaseInvoice update(@PathVariable Long id, @RequestBody PurchaseInvoiceRequest request) {
        return purchaseInvoiceService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        purchaseInvoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
