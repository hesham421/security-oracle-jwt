package com.example.security.controller;

import com.example.security.domain.Role;
import com.example.security.dto.AssignPermissionsRequest;
import com.example.security.dto.CreateRoleRequest;
import com.example.security.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // TODO: إنشاء دور جديد - يتطلب PERM_ROLE_CREATE
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_ROLE_CREATE')")
    public Role create(@RequestBody @Valid CreateRoleRequest req) {
        return roleService.createRole(req);
    }

    // TODO: ربط صلاحيات بالدور - يتطلب PERM_ROLE_CREATE (أو PERM_ROLE_UPDATE إذا أردتِ إنشاءها)
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_ROLE_CREATE')")
    public Role assign(@PathVariable Long id, @RequestBody @Valid AssignPermissionsRequest req) {
        return roleService.assignPermissions(id, req);
    }

    // TODO: عرض جميع الأدوار - يتطلب PERM_ROLE_VIEW
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ROLE_VIEW')")
    public List<Role> all() {
        return roleService.listRoles();
    }

    // TODO: حذف دور - يتطلب PERM_ROLE_DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_ROLE_DELETE')")
    public void delete(@PathVariable Long id) {
        roleService.deleteRole(id);
    }
}
