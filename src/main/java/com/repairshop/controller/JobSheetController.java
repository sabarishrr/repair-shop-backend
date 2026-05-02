package com.repairshop.controller;

import com.repairshop.dto.DashboardStats;
import com.repairshop.dto.JobSheetRequest;
import com.repairshop.dto.StatusUpdateRequest;
import com.repairshop.model.JobSheet;
import com.repairshop.model.JobStatus;
import com.repairshop.service.JobSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobSheetController {

    private final JobSheetService jobSheetService;

    @GetMapping("/stats")
    public DashboardStats getStats() {
        return jobSheetService.getStats();
    }

    @GetMapping
    public List<JobSheet> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) JobStatus status) {
        if (search != null && !search.isBlank()) {
            return jobSheetService.search(search);
        }
        if (status != null) {
            return jobSheetService.getByStatus(status);
        }
        return jobSheetService.getAll();
    }

    @GetMapping("/{id}")
    public JobSheet getById(@PathVariable Long id) {
        return jobSheetService.getById(id);
    }

    @PostMapping
    public JobSheet create(@RequestBody JobSheetRequest request) {
        return jobSheetService.create(request);
    }

    @PutMapping("/{id}")
    public JobSheet update(@PathVariable Long id, @RequestBody JobSheetRequest request) {
        return jobSheetService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public JobSheet updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        return jobSheetService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobSheetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
