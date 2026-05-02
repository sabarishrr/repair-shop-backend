package com.repairshop.dto;

import com.repairshop.model.JobStatus;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    private JobStatus status;
    private String notes;
}
