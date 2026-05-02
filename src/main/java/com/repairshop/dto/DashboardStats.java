package com.repairshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class DashboardStats {
    private long totalJobs;
    private long received;
    private long diagnosing;
    private long awaitingParts;
    private long inRepair;
    private long readyForPickup;
    private long delivered;
    private long pendingJobs;
    private long deliveredToday;
    private BigDecimal totalRevenue;
    private List<?> recentJobs;
}
