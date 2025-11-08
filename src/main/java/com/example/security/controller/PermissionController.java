package com.example.security.controller;

import com.example.security.domain.Permission;
import com.example.security.dto.CreatePermissionRequest;
import com.example.security.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // TODO: إنشاء صلاحية جديدة - يتطلب PERM_PERMISSION_CREATE
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_PERMISSION_CREATE')")
    public Permission create(@RequestBody @Valid CreatePermissionRequest req) {
        return permissionService.createPermission(req);
    }

    // TODO: عرض جميع الصلاحيات - يتطلب PERM_PERMISSION_VIEW
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_PERMISSION_VIEW')")
    public List<Permission> all() {
        System.out.println("inside all::::::::::::");
        return permissionService.listPermissions();
    }

    // TODO: حذف صلاحية - يتطلب PERM_PERMISSION_DELETE
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_PERMISSION_DELETE')")
    public void delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
    }
}
