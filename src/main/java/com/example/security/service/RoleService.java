package com.example.security.service;

import com.example.security.domain.Permission;
import com.example.security.domain.Role;
import com.example.security.dto.AssignPermissionsRequest;
import com.example.security.dto.CreateRoleRequest;
import com.example.security.multitenancy.TenantContext;
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
        String tenant = requireTenant();

        roleRepo.findByNameAndTenantId(req.getName(), tenant).ifPresent(r -> {
            throw new IllegalArgumentException("Role already exists in tenant: " + req.getName());
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
        String tenant = requireTenant();

        Role role = roleRepo.findByIdAndTenantId(roleId, tenant)
                .orElseThrow(() -> new IllegalArgumentException("Role not found in current tenant: " + roleId));

        Set<Permission> perms = new HashSet<>();
        for (String name : req.getPermissions()) {
            Permission p = permRepo.findByNameAndTenantId(name, tenant)
                    .orElseThrow(() -> new IllegalArgumentException("Permission not found in current tenant: " + name));
            perms.add(p);
        }
        role.setPermissions(perms);
        return roleRepo.save(role);
    }

    // عرض جميع الأدوار للـ tenant الحالي
    public List<Role> listRoles() {
        return roleRepo.findAllByTenantId(requireTenant());
    }

    // حذف دور داخل الـ tenant الحالي
    @Transactional
    public void deleteRole(Long id) {
        long deleted = roleRepo.deleteByIdAndTenantId(id, requireTenant());
        if (deleted == 0) {
            throw new IllegalArgumentException("Role not found for current tenant");
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
