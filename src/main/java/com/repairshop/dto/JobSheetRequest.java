package com.repairshop.dto;

import com.repairshop.model.JobStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class JobSheetRequest {
    private Long customerId;
    private String deviceType;
    private String brand;
    private String model;
    private String serialNumber;
    private String problemDescription;
    private String accessories;
    private String technician;
    private BigDecimal estimatedCost;
    private BigDecimal finalCost;
    private JobStatus status;
    private String notes;
    private LocalDate receivedDate;
    private LocalDate deliveryDate;
}
