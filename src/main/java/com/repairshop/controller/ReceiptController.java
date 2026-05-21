package com.repairshop.controller;

import com.repairshop.dto.ReceiptRequest;
import com.repairshop.model.Receipt;
import com.repairshop.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping
    public List<Receipt> getAll() {
        return receiptService.getAll();
    }

    @GetMapping("/{id}")
    public Receipt getById(@PathVariable Long id) {
        return receiptService.getById(id);
    }

    @PostMapping
    public Receipt create(@RequestBody ReceiptRequest request) {
        return receiptService.create(request);
    }

    @PutMapping("/{id}")
    public Receipt update(@PathVariable Long id, @RequestBody ReceiptRequest request) {
        return receiptService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        receiptService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
