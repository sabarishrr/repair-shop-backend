package com.repairshop.controller;

import com.repairshop.dto.ExtractedInvoiceResponse;
import com.repairshop.service.InvoiceExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InvoiceExtractionController {

    private final InvoiceExtractionService invoiceExtractionService;

    @PostMapping("/extract")
    public ResponseEntity<?> extractInvoice(@RequestParam("file") MultipartFile file) {
        try {
            ExtractedInvoiceResponse response = invoiceExtractionService.extractInvoice(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

