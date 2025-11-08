package com.example.security.repo;

import com.example.security.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByNameAndTenantId(String name, String tenantId);

    boolean existsByNameAndTenantId(String name, String tenantId);

    List<Permission> findAllByTenantId(String tenantId);

    long deleteByIdAndTenantId(Long id, String tenantId);
}
