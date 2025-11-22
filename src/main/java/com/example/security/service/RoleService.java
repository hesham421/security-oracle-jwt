package com.example.security.service;

import com.example.security.common.LocalizedException;
import com.example.security.domain.Permission;
import com.example.security.domain.Role;
import com.example.security.dto.AssignPermissionsRequest;
import com.example.security.dto.CreateRoleRequest;
import com.example.security.multitenancy.TenantContext;
import com.example.security.common.TenantHelper;
import com.example.security.repo.PermissionRepository;
import com.example.security.repo.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    // إنشاء دور جديد (محصور بالـ tenant)
    @Transactional
    public Role createRole(CreateRoleRequest req) {
        String tenant = TenantHelper.requireTenant();

        roleRepo.findByNameAndTenantId(req.getName(), tenant).ifPresent(r -> {
            throw new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "ROLE_ALREADY_EXISTS", req.getName());
        });

        Role r = Role.builder()
                .tenantId(tenant)
                .name(req.getName())           // تأكد إن الاسم بصيغة ROLE_*
                .permissions(new HashSet<>())
                .build();

        return roleRepo.save(r);
    }

    // ربط صلاحيات بدور (استبدال القائمة كاملة) — tenant-aware
    @Transactional
    public Role assignPermissions(Long roleId, AssignPermissionsRequest req) {
        String tenant = TenantHelper.requireTenant();

    Role role = roleRepo.findByIdAndTenantId(roleId, tenant)
        .orElseThrow(() -> new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", roleId));

        Set<Permission> perms = new HashSet<>();
        for (String name : req.getPermissions()) {
        Permission p = permRepo.findByNameAndTenantId(name, tenant)
            .orElseThrow(() -> new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "PERMISSION_NOT_FOUND", name));
            perms.add(p);
        }
        role.setPermissions(perms);
        return roleRepo.save(role);
    }

    // عرض جميع الأدوار للـ tenant الحالي
    public List<Role> listRoles() {
    return roleRepo.findAllByTenantId(TenantHelper.requireTenant());
    }

    // حذف دور داخل الـ tenant الحالي
    @Transactional
    public void deleteRole(Long id) {
        long deleted = roleRepo.deleteByIdAndTenantId(id, TenantHelper.requireTenant());
        if (deleted == 0) {
            throw new LocalizedException(org.springframework.http.HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND_FOR_TENANT", id);
        }
    }

    // tenant checking delegated to TenantHelper
}
