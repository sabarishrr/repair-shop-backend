package com.repairshop.repository;

import com.repairshop.model.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long> {
    List<CreditNote> findAllByOrderByNoteDateDesc();
    List<CreditNote> findByNoteDateBetweenOrderByNoteDateDesc(LocalDate startDate, LocalDate endDate);
}

