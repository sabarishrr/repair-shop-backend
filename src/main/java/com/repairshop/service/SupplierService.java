package com.repairshop.service;

import com.repairshop.dto.SupplierRequest;
import com.repairshop.model.Supplier;
import com.repairshop.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public List<Supplier> getAll() {
        return supplierRepository.findAllByOrderByNameAsc();
    }

    public Supplier getById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + id));
    }

    public Supplier create(SupplierRequest req) {
        Supplier s = new Supplier();
        return save(s, req);
    }

    public Supplier update(Long id, SupplierRequest req) {
        Supplier s = getById(id);
        return save(s, req);
    }

    private Supplier save(Supplier s, SupplierRequest req) {
        s.setName(req.getName());
        s.setGstin(req.getGstin());
        s.setPhone(req.getPhone());
        s.setEmail(req.getEmail());
        s.setAddress(req.getAddress());
        s.setState(req.getState());
        return supplierRepository.save(s);
    }

    public void delete(Long id) {
        supplierRepository.deleteById(id);
    }
}
