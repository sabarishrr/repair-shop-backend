package com.repairshop.service;

import com.repairshop.model.Product;
import com.repairshop.model.StockAdjustment;
import com.repairshop.repository.ProductRepository;
import com.repairshop.repository.StockAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockAdjustmentService {

    private final StockAdjustmentRepository adjustmentRepository;
    private final ProductRepository productRepository;

    public List<StockAdjustment> getAllAdjustments() {
        return adjustmentRepository.findAllByOrderByAdjustmentDateDesc();
    }

    @Transactional
    public StockAdjustment createAdjustment(StockAdjustment adjustment) {
        if (adjustment.getProduct() == null || adjustment.getProduct().getId() == null) {
            throw new IllegalArgumentException("Product ID is required for stock adjustment");
        }

        Product product = productRepository.findById(adjustment.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + adjustment.getProduct().getId()));

        adjustment.setAdjustmentDate(LocalDateTime.now());
        adjustment.setProduct(product);

        int qty = adjustment.getQuantity();
        if (adjustment.getAdjustmentType() == StockAdjustment.AdjustmentType.SUBTRACT) {
            product.setStockQuantity(product.getStockQuantity() - qty);
        } else if (adjustment.getAdjustmentType() == StockAdjustment.AdjustmentType.ADD) {
            product.setStockQuantity(product.getStockQuantity() + qty);
        }

        productRepository.save(product);
        return adjustmentRepository.save(adjustment);
    }
}
