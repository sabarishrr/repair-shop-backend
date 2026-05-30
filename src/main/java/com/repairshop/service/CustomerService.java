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

    public List<Customer> getActive() {
        return customerRepository.findByActiveTrueOrderByNameAsc();
    }

    public Customer toggleActive(Long id) {
        Customer c = getById(id);
        c.setActive(!c.isActive());
        return customerRepository.save(c);
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
        c.setCompanyName(req.getCompanyName());
        c.setGstin(req.getGstin());
        c.setState(req.getState());
        c.setCustomerType(req.getCustomerType() != null ? req.getCustomerType() : "UNREGISTERED");
        c.setPinCode(req.getPinCode());
        c.setShippingAddress(req.getShippingAddress());
        c.setShippingPinCode(req.getShippingPinCode());
        return customerRepository.save(c);
    }

    public void delete(Long id) {
        customerRepository.deleteById(id);
    }
}
