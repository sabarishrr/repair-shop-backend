package com.repairshop.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "common_issues")
@Data
public class CommonIssue extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String issue;
}
