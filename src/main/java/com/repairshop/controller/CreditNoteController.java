package com.repairshop.controller;

import com.repairshop.dto.CreditNoteRequest;
import com.repairshop.model.CreditNote;
import com.repairshop.service.CreditNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-notes")
@RequiredArgsConstructor
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    @GetMapping
    public List<CreditNote> getAll() {
        return creditNoteService.getAll();
    }

    @GetMapping("/{id}")
    public CreditNote getById(@PathVariable Long id) {
        return creditNoteService.getById(id);
    }

    @PostMapping
    public CreditNote create(@RequestBody CreditNoteRequest request) {
        return creditNoteService.create(request);
    }

    @PostMapping("/{id}/cancel")
    public CreditNote cancel(@PathVariable Long id) {
        return creditNoteService.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        creditNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
