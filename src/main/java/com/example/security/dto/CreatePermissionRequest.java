package com.example.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePermissionRequest {
    // TODO: اسم الصلاحية (مثال: PERM_ROLE_VIEW)
    @NotBlank private String name;
}
