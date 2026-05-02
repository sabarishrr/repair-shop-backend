package com.repairshop.service;

import com.repairshop.dto.CustomerRequest;
import com.repairshop.model.Customer;
import com.repairshop.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public List<Customer> search(String query) {
        return customerRepository.findByNameContainingIgnoreCaseOrPhoneContaining(query, query);
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }

    public Customer create(CustomerRequest req) {
        Customer c = new Customer();
        return save(c, req);
    }

    public Customer update(Long id, CustomerRequest req) {
        Customer c = getById(id);
        return save(c, req);
    }

    private Customer save(Customer c, CustomerRequest req) {
        c.setName(req.getName());
        c.setPhone(req.getPhone());
        c.setEmail(req.getEmail());
        c.setAddress(req.getAddress());
        c.setState(req.getState());
        return customerRepository.save(c);
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }
}
