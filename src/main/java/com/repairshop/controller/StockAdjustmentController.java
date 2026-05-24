package com.repairshop.controller;

import com.repairshop.model.StockAdjustment;
import com.repairshop.service.StockAdjustmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-adjustments")
@RequiredArgsConstructor
public class StockAdjustmentController {

    private final StockAdjustmentService service;

    @GetMapping
    public List<StockAdjustment> getAll() {
        return service.getAllAdjustments();
    }

    @PostMapping
    public StockAdjustment create(@RequestBody StockAdjustment adjustment) {
        return service.createAdjustment(adjustment);
    }
}
