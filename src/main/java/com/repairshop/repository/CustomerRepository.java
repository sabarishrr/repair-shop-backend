package com.repairshop.repository;

import com.repairshop.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);
    List<Customer> findByActiveTrueOrderByNameAsc();
}
