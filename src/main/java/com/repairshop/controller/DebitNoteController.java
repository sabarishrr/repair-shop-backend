package com.repairshop.controller;

import com.repairshop.dto.DebitNoteRequest;
import com.repairshop.model.DebitNote;
import com.repairshop.service.DebitNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debit-notes")
@RequiredArgsConstructor
public class DebitNoteController {

    private final DebitNoteService debitNoteService;

    @GetMapping
    public List<DebitNote> getAll() {
        return debitNoteService.getAll();
    }

    @GetMapping("/{id}")
    public DebitNote getById(@PathVariable Long id) {
        return debitNoteService.getById(id);
    }

    @PostMapping
    public DebitNote create(@RequestBody DebitNoteRequest request) {
        return debitNoteService.create(request);
    }

    @PostMapping("/{id}/cancel")
    public DebitNote cancel(@PathVariable Long id) {
        return debitNoteService.cancel(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        debitNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
