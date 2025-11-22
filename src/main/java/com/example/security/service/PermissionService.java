package com.example.security.service;

import com.example.security.common.LocalizedException;
import com.example.security.domain.Permission;
import com.example.security.dto.CreatePermissionRequest;
import com.example.security.multitenancy.TenantContext;
import com.example.security.common.TenantHelper;
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
        String tenant = TenantHelper.requireTenant();
        // تحقق من عدم التكرار داخل نفس الـtenant
        permRepo.findByNameAndTenantId(req.getName(), tenant).ifPresent(p -> {
            throw new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "PERMISSION_ALREADY_EXISTS", req.getName());
        });
        Permission p = Permission.builder()
                .tenantId(tenant)
                .name(req.getName())
                .build();
        return permRepo.save(p);
    }

    public List<Permission> listPermissions() {
        return permRepo.findAllByTenantId(TenantHelper.requireTenant());
    }

    @Transactional
    public void deletePermission(Long id) {
        long deleted = permRepo.deleteByIdAndTenantId(id, TenantHelper.requireTenant());
        if (deleted == 0) {
            throw new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "PERMISSION_NOT_FOUND_FOR_TENANT", id);
        }
    }
    // tenant checking delegated to TenantHelper
}
