package com.repairshop.repository;

import com.repairshop.model.CommonIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommonIssueRepository extends JpaRepository<CommonIssue, Long> {
}
