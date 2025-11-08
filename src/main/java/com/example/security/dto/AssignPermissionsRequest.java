package com.example.security.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class AssignPermissionsRequest {
    // TODO: أسماء الصلاحيات المراد ربطها بالدور
    @NotEmpty private Set<String> permissions;
}
