package com.repairshop.repository;

import com.repairshop.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findAllByOrderByReceiptDateDesc();
    List<Receipt> findBySalesInvoiceId(Long salesInvoiceId);
    List<Receipt> findByReceiptDateBetweenOrderByReceiptDateDesc(LocalDate startDate, LocalDate endDate);
}

