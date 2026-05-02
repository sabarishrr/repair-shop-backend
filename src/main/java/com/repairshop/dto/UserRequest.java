package com.repairshop.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String password; // Nullable when updating
    private String fullName;
    private String role; // "ADMIN" or "TECHNICIAN"
}
