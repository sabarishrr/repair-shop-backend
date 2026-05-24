package com.repairshop.repository;

import com.repairshop.model.DebitNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {
    List<DebitNote> findAllByOrderByNoteDateDesc();
    List<DebitNote> findByNoteDateBetweenOrderByNoteDateDesc(LocalDate startDate, LocalDate endDate);
}

