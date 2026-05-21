package com.repairshop.dto;

import com.repairshop.model.State;
import lombok.Data;

@Data
public class SupplierRequest {
    private String name;
    private String gstin;
    private String phone;
    private String email;
    private String address;
    private State state;
}
