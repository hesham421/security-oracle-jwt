package com.example.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleRequest {
    // TODO: اسم الدور (مثال: ROLE_ACCOUNTANT)
    @NotBlank private String name;
}
