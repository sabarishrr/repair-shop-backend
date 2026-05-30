package com.repairshop.dto;

import com.repairshop.model.JobStatus;
import com.repairshop.model.PaymentStatus;
import com.repairshop.model.PaymentMethod;
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
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String notes;
    private String materialUsed;
    private String actionTaken;
    private LocalDate receivedDate;
    private LocalDate deliveryDate;
}

