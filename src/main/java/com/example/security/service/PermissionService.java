package com.example.security.service;

import com.example.security.domain.Permission;
import com.example.security.dto.CreatePermissionRequest;
import com.example.security.multitenancy.TenantContext;
import com.example.security.repo.PermissionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permRepo;

    @Transactional
    public Permission createPermission(CreatePermissionRequest req) {
        String tenant = requireTenant();
        // تحقق من عدم التكرار داخل نفس الـtenant
        permRepo.findByNameAndTenantId(req.getName(), tenant).ifPresent(p -> {
            throw new IllegalArgumentException("Permission already exists in tenant: " + req.getName());
        });
        Permission p = Permission.builder()
                .tenantId(tenant)
                .name(req.getName())
                .build();
        return permRepo.save(p);
    }

    public List<Permission> listPermissions() {
        return permRepo.findAllByTenantId(requireTenant());
    }

    @Transactional
    public void deletePermission(Long id) {
        long deleted = permRepo.deleteByIdAndTenantId(id, requireTenant());
        if (deleted == 0) {
            throw new IllegalArgumentException("Permission not found for current tenant");
        }
    }

    private String requireTenant() {
        String t = TenantContext.getTenantId();
        if (t == null || t.isBlank()) {
            throw new IllegalStateException("Missing tenant in context");
        }
        return t;
    }
}
