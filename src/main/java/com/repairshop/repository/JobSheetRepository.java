package com.repairshop.repository;

import com.repairshop.model.JobSheet;
import com.repairshop.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface JobSheetRepository extends JpaRepository<JobSheet, Long> {

    List<JobSheet> findAllByOrderByCreatedAtDesc();

    List<JobSheet> findTop10ByOrderByCreatedAtDesc();

    List<JobSheet> findByStatusOrderByCreatedAtDesc(JobStatus status);

    @Query("SELECT j FROM JobSheet j WHERE LOWER(j.jobNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(j.customer.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(j.customer.phone) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY j.createdAt DESC")
    List<JobSheet> search(String query);

    long countByStatus(JobStatus status);

    @Query("SELECT SUM(j.finalCost) FROM JobSheet j WHERE j.status = 'DELIVERED'")
    BigDecimal sumTotalRevenue();

    // To calculate delivered today, we might need a more specific query based on date.
    @Query(value = "SELECT COUNT(*) FROM job_sheets WHERE status = 'DELIVERED' AND delivered_date = CURDATE()", nativeQuery = true)
    long countDeliveredToday();
}