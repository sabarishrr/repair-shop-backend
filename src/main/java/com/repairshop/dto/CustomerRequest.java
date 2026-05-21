package com.repairshop.dto;

import lombok.Data;

import com.repairshop.model.State;

@Data
public class CustomerRequest {
    private String name;
    private String phone;
    private String email;
    private String address;
    private String companyName;
    private String gstin;
    private State state;
    private String customerType;
    private String pinCode;
    private String shippingAddress;
    private String shippingPinCode;
}
