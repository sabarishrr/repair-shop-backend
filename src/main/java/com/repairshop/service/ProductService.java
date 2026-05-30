package com.repairshop.service;

import com.repairshop.model.Product;
import com.repairshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    public List<Product> getAllProducts(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return repository.findByNameContainingIgnoreCase(search);
        }
        return repository.findAll();
    }

    public List<Product> getActiveProducts(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return repository.findByActiveTrueAndNameContainingIgnoreCase(search);
        }
        return repository.findByActiveTrue();
    }

    public Product toggleActive(Long id) {
        Product p = getProductById(id);
        p.setActive(!p.isActive());
        return repository.save(p);
    }

    public Product getProductById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product createProduct(Product product) {
        return repository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        Product existing = getProductById(id);
        existing.setName(product.getName());
        existing.setDescription(product.getDescription());
        existing.setRate(product.getRate());
        existing.setHsn(product.getHsn());
        existing.setGstPercentage(product.getGstPercentage());
        existing.setUom(product.getUom());
        existing.setPurchasePrice(product.getPurchasePrice());
        existing.setMrp(product.getMrp());
        existing.setWholesalePrice(product.getWholesalePrice());
        existing.setReorderLevel(product.getReorderLevel());
        return repository.save(existing);
    }

    public void deleteProduct(Long id) {
        repository.deleteById(id);
    }
}
